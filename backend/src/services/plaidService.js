const { Configuration, PlaidApi, PlaidEnvironments } = require('plaid');
const plaidRepository = require('../repositories/plaidRepository');
const logger = require('../utils/logger');
const cache = require('../utils/cache');
const llmService = require('./llmService');
const retry = require('../utils/retry');

class PlaidService {
  constructor() {
    this.client = null;
    this.initialize();
  }

  initialize() {
    const configuration = new Configuration({
      basePath: PlaidEnvironments[process.env.PLAID_ENV || 'sandbox'],
      baseOptions: {
        headers: {
          'PLAID-CLIENT-ID': process.env.PLAID_CLIENT_ID,
          'PLAID-SECRET': process.env.PLAID_SECRET,
        },
      },
    });

    this.client = new PlaidApi(configuration);
    logger.info(`Plaid client initialized with environment: ${process.env.PLAID_ENV || 'sandbox'}`);
  }

  /**
   * Create a Link token for initializing Plaid Link
   */
  async createLinkToken(userId) {
    try {
      const linkTokenConfig = {
        user: {
          client_user_id: userId.toString(),
        },
        client_name: 'Credit Card Optimizer',
        products: ['auth', 'transactions', 'liabilities'], // Added liabilities for statement dates
        country_codes: ['US'],
        language: 'en',
        // Android app configuration for OAuth redirects
        android_package_name: 'im.ilyk.ccoptimizer',
      };

      // For mobile apps, DO NOT set redirect_uri - the Plaid SDK handles OAuth internally
      // Only set redirect_uri if explicitly configured (for web apps)
      if (process.env.PLAID_REDIRECT_URI) {
        linkTokenConfig.redirect_uri = process.env.PLAID_REDIRECT_URI;
        logger.debug(`Using redirect URI: ${process.env.PLAID_REDIRECT_URI}`);
      } else {
        logger.debug('No redirect URI configured - using Plaid SDK default (mobile app)');
      }

      // Only add webhook if it's properly configured (optional for sandbox)
      if (process.env.PLAID_WEBHOOK_URL && 
          process.env.PLAID_WEBHOOK_URL.startsWith('http')) {
        linkTokenConfig.webhook = process.env.PLAID_WEBHOOK_URL;
        logger.debug(`Using webhook URL: ${process.env.PLAID_WEBHOOK_URL}`);
      } else {
        logger.debug('No webhook URL configured (optional for sandbox)');
      }

      const response = await this.client.linkTokenCreate(linkTokenConfig);

      return {
        link_token: response.data.link_token,
        expiration: response.data.expiration,
      };
    } catch (error) {
      logger.error('Error creating link token:', error.response?.data || error.message);
      throw error;
    }
  }

  /**
   * Exchange public token for access token
   */
  async exchangePublicToken(userId, publicToken) {
    try {
      const response = await this.client.itemPublicTokenExchange({
        public_token: publicToken,
      });

      const accessToken = response.data.access_token;
      const itemId = response.data.item_id;

      // Store encrypted access token in database
      await plaidRepository.storeAccessToken(userId, itemId, accessToken);

      logger.info(`Access token stored for user ${userId}, item ${itemId}`);

      return {
        itemId,
        success: true,
      };
    } catch (error) {
      logger.error('Error exchanging public token:', error);
      throw error;
    }
  }

  /**
   * Analyze balance changes to find statement dates
   * Some banks don't have statement marker transactions, but balances reset
   */
  analyzeBalancePattern(transactions, accountId) {
    const accountTransactions = transactions
      .filter(t => t.account_id === accountId)
      .sort((a, b) => new Date(a.date) - new Date(b.date));
    
    if (accountTransactions.length < 30) {
      return null; // Need enough data
    }

    // Group transactions by month
    const monthlyGroups = {};
    accountTransactions.forEach(t => {
      const date = new Date(t.date);
      const monthKey = `${date.getFullYear()}-${date.getMonth()}`;
      if (!monthlyGroups[monthKey]) {
        monthlyGroups[monthKey] = [];
      }
      monthlyGroups[monthKey].push({ date: date.getDate(), amount: t.amount });
    });

    // Look for days with high transaction volume (statement posts)
    const candidateDays = [];
    Object.values(monthlyGroups).forEach(monthTransactions => {
      // Find day with most transactions (statement day often has multiple items posting)
      const dayCounts = {};
      monthTransactions.forEach(t => {
        dayCounts[t.date] = (dayCounts[t.date] || 0) + 1;
      });
      
      const maxDay = Object.keys(dayCounts)
        .reduce((a, b) => dayCounts[a] > dayCounts[b] ? a : b, null);
      
      if (maxDay && dayCounts[maxDay] >= 3) { // At least 3 transactions
        candidateDays.push(parseInt(maxDay));
      }
    });

    if (candidateDays.length >= 2) {
      // Find most common day
      const dayCounts = {};
      candidateDays.forEach(day => {
        dayCounts[day] = (dayCounts[day] || 0) + 1;
      });
      
      const mostCommonDay = Object.keys(dayCounts)
        .reduce((a, b) => dayCounts[a] > dayCounts[b] ? a : b);
      
      const parsedDay = parseInt(mostCommonDay);
      if (parsedDay >= 1 && parsedDay <= 31) {
        logger.info(`Found statement day ${parsedDay} from balance pattern analysis`);
        return parsedDay;
      }
    }

    return null;
  }

  /**
   * Analyze transaction history to determine recurring statement date
   * Uses multiple methods with fallbacks for maximum reliability
   * Now with LLM intelligence - NO hardcoded string matching!
   */
  async analyzeStatementPattern(transactions, accountId, accountInfo = {}) {
    // Check cache first (cache for 30 days)
    const cacheKey = `statement_day:${accountId}`;
    const cached = cache.get(cacheKey);
    if (cached !== null) {
      logger.debug(`Using cached statement day ${cached} for account ${accountId}`);
      return cached;
    }

    // Filter transactions for this account
    const accountTransactions = transactions.filter(t => t.account_id === accountId);
    
    if (accountTransactions.length === 0) {
      return null;
    }

    let detectedDay = null;

    // METHOD 1: LLM Analysis (most intelligent - NO string matching!)
    if (llmService.isAvailable()) {
      try {
        logger.info(`[Method 1] Trying LLM analysis (${llmService.getProvider()}) for account ${accountId}`);
        detectedDay = await llmService.analyzeTransactionsWithLLM(accountTransactions, accountInfo);
        
        if (detectedDay && detectedDay >= 1 && detectedDay <= 31) {
          logger.info(`[Method 1] LLM detected statement day ${detectedDay} for account ${accountId}`);
          // Cache for 30 days
          cache.set(cacheKey, detectedDay, 30 * 24 * 60 * 60 * 1000);
          return detectedDay;
        }
      } catch (error) {
        logger.warn(`[Method 1] LLM analysis failed: ${error.message}, trying fallback methods`);
      }
    }
    
    // METHOD 2: Transaction volume/pattern analysis (fallback)
    logger.info(`[Method 2] Trying balance pattern analysis for account ${accountId}`);
    detectedDay = this.analyzeBalancePattern(accountTransactions, accountId);
    if (detectedDay) {
      logger.info(`[Method 2] Found statement day ${detectedDay} from balance pattern`);
      // Cache for 30 days
      cache.set(cacheKey, detectedDay, 30 * 24 * 60 * 60 * 1000);
      return detectedDay;
    }
    
    // All methods failed - cache the null result too (for 1 day, try again tomorrow)
    logger.warn(`All analysis methods failed for account ${accountId} - user input required`);
    cache.set(cacheKey, null, 24 * 60 * 60 * 1000);
    return null;
  }

  /**
   * Fetch all transactions with pagination support
   * Plaid returns max 500 transactions per call, need to paginate
   */
  async fetchAllTransactions(accessToken, startDate, endDate) {
    const allTransactions = [];
    let hasMore = true;
    let offset = 0;
    const MAX_TRANSACTIONS_PER_REQUEST = 500;

    while (hasMore) {
      try {
        const response = await this.client.transactionsGet({
          access_token: accessToken,
          start_date: startDate,
          end_date: endDate,
          options: {
            count: MAX_TRANSACTIONS_PER_REQUEST,
            offset: offset,
          }
        });

        const transactions = response.data.transactions;
        allTransactions.push(...transactions);

        // Check if there are more transactions
        const totalTransactions = response.data.total_transactions;
        offset += transactions.length;
        hasMore = offset < totalTransactions;

        if (hasMore) {
          logger.info(`Fetched ${offset}/${totalTransactions} transactions, continuing...`);
        }
      } catch (error) {
        logger.warn(`Transaction pagination failed at offset ${offset}:`, error.message);
        hasMore = false; // Stop pagination on error
      }
    }

    logger.info(`Fetched total of ${allTransactions.length} transactions`);
    return { data: { transactions: allTransactions } };
  }

  /**
   * Fetch accounts for a single item (helper for parallel processing)
   */
  async fetchAccountsForItem(item, userId) {
    try {
      const tokenData = await plaidRepository.getAccessToken(item.item_id);
      
      if (!tokenData) {
        logger.warn(`No access token found for item ${item.item_id}`);
        return [];
      }
      
      // Fetch institution data for branding (logo, color)
      // First, get the institution_id from the item
      let institutionData = null;
      let institutionId = null;
      
      try {
        // Get item details to fetch institution_id
        const itemResponse = await this.client.itemGet({ access_token: tokenData.accessToken });
        institutionId = itemResponse.data.item.institution_id;
        
        if (institutionId) {
          const institutionResponse = await this.client.institutionsGetById({
            institution_id: institutionId,
            country_codes: ['US'],
            options: {
              include_optional_metadata: true
            }
          });
          institutionData = institutionResponse.data.institution;
          logger.debug(`Fetched institution data for ${institutionData.name}`);
        }
      } catch (error) {
        logger.warn(`Failed to fetch institution data: ${error.message}`);
        // Continue without institution data
      }

      const SIX_MONTHS_MS = 180 * 24 * 60 * 60 * 1000;
      const endDate = new Date().toISOString().split('T')[0];
      const startDate = new Date(Date.now() - SIX_MONTHS_MS).toISOString().split('T')[0];

      // Fetch all data in parallel with retry logic
      const [accountsResponse, liabilitiesData, transactions] = await Promise.allSettled([
        retry.retry(
          () => this.client.accountsGet({ access_token: tokenData.accessToken }),
          { maxAttempts: 3, initialDelayMs: 1000 }
        ),
        retry.retry(
          () => this.client.liabilitiesGet({ access_token: tokenData.accessToken }),
          { maxAttempts: 2, initialDelayMs: 500 }
        ),
        retry.retry(
          () => this.fetchAllTransactions(tokenData.accessToken, startDate, endDate),
          { maxAttempts: 3, initialDelayMs: 1000 }
        )
      ]);

      // Handle accounts response
      if (accountsResponse.status !== 'fulfilled') {
        logger.error(`Failed to fetch accounts for item ${item.item_id}`);
        return [];
      }

      // Extract liabilities data (if available)
      const liabilities = liabilitiesData.status === 'fulfilled' 
        ? liabilitiesData.value.data.liabilities 
        : null;
      
      if (liabilitiesData.status === 'rejected') {
        logger.warn(`Liabilities not available for item ${item.item_id}`);
      }

      // Extract transactions (if available)
      const txList = transactions.status === 'fulfilled'
        ? transactions.value.data.transactions
        : [];
      
      if (transactions.status === 'rejected') {
        logger.warn(`Transactions not available for item ${item.item_id}`);
      }

      // Process credit card accounts
      const creditCardAccounts = accountsResponse.value.data.accounts
        .filter(account => account.type === 'credit');

      // Analyze statement days in parallel for all accounts
      const accounts = await Promise.all(
        creditCardAccounts.map(async (account) => {
          const liability = liabilities?.credit?.find(
            cc => cc.account_id === account.account_id
          );

          // Check for manually saved statement date first
          const savedCard = await plaidRepository.getCreditCard(userId, account.account_id);
          let finalStatementDay = null;
          
          if (savedCard && savedCard.statement_date) {
            // Use manually saved statement date
            finalStatementDay = savedCard.statement_date;
            logger.info(`Using saved statement date ${finalStatementDay} for account ${account.account_id}`);
          } else {
            // Analyze from transactions if no saved date
            const accountInfo = {
              name: account.name,
              bankName: account.official_name || account.name
            };
            
            finalStatementDay = await this.analyzeStatementPattern(txList, account.account_id, accountInfo);
          }

          // **Epic 4: Get manual payments for this card**
          const manualPayments = await plaidRepository.getManualPayments(userId, account.account_id);
          
          // Calculate total manual payments (for current cycle or all time)
          const totalManualPaymentsCents = manualPayments.reduce((sum, p) => sum + p.amountCents, 0);
          const totalManualPayments = totalManualPaymentsCents / 100;
          
          // Calculate adjusted balance (current balance - manual payments)
          const currentBalance = account.balances.current || 0;
          const adjustedBalance = Math.max(0, currentBalance - totalManualPayments);
          
          if (manualPayments.length > 0) {
            logger.info(
              `Card ${account.account_id}: ${manualPayments.length} manual payments found. ` +
              `Total: $${totalManualPayments}, Current: $${currentBalance}, Adjusted: $${adjustedBalance}`
            );
          }

          // **SAVE CARD TO DATABASE** for manual payment foreign key validation
          // This ensures credit_cards table is always up-to-date with Plaid data
          try {
            await plaidRepository.saveCreditCard(userId, {
              account_id: account.account_id,
              name: account.name,
              mask: account.mask,
              credit_limit: Math.round((account.balances.limit || 0) * 100), // Convert to cents
              statement_date: finalStatementDay,
              due_date: liability?.next_payment_due_date || null,
              auto_pay_enabled: false,  // Will be 0 in SQLite
              auto_pay_amount: null
            });
            logger.debug(`Saved card ${account.account_id} to credit_cards table`);
          } catch (saveError) {
            logger.error(`Failed to save card ${account.account_id} to database:`, saveError);
            // Don't fail the whole sync if save fails
          }

          return {
            accountId: account.account_id,
            name: account.name,
            officialName: account.official_name,
            mask: account.mask,
            type: account.type,
            subtype: account.subtype,
            balances: {
              available: account.balances.available,
              current: account.balances.current,
              limit: account.balances.limit,
              // **Epic 4: Add manual payment info**
              manualPaymentsTotal: totalManualPayments,
              adjustedCurrent: adjustedBalance,  // Balance after manual payments
            },
            liability: liability ? {
              lastStatementDate: liability.last_statement_issue_date,
              nextPaymentDueDate: liability.next_payment_due_date,
              lastStatementBalance: liability.last_statement_balance,
              minimumPaymentAmount: liability.minimum_payment_amount,
              recurringStatementDay: finalStatementDay,
            } : {
              recurringStatementDay: finalStatementDay,
            },
            // **Epic 4: Add manual payment details**
            manualPayments: manualPayments.map(p => ({
              id: p.id,
              date: p.date,
              amountCents: p.amountCents,
              source: p.source
            })),
            // Add institution branding for card artwork
            institution: institutionData ? {
              institutionId: institutionData.institution_id,
              name: institutionData.name,
              logo: institutionData.logo,  // PNG logo URL from Plaid CDN
              primaryColor: institutionData.primary_color,  // Hex color
              url: institutionData.url
            } : null,
          };
        })
      );

      return accounts;
    } catch (error) {
      logger.error(`Error fetching accounts for item ${item.item_id}:`, error);
      return [];
    }
  }

  /**
   * Get accounts for a user with liability data and statement date analysis
   * Now uses parallel processing for multiple bank connections
   */
  async getAccounts(userId) {
    try {
      // Get all Plaid items for the user
      const items = await plaidRepository.getUserItems(userId);
      
      if (!items || items.length === 0) {
        return [];
      }

      // Process all items in parallel (massive performance improvement)
      const accountArrays = await Promise.all(
        items.map(item => this.fetchAccountsForItem(item, userId))
      );

      // Flatten arrays and return
      const allAccounts = accountArrays.flat();
      
      logger.info(`Fetched ${allAccounts.length} credit card(s) for user ${userId} from ${items.length} bank connection(s)`);
      
      return allAccounts;
    } catch (error) {
      logger.error('Error getting accounts:', error);
      throw error;
    }
  }

  /**
   * Get transactions for a user
   */
  async getTransactions(userId, startDate, endDate) {
    try {
      const items = await plaidRepository.getUserItems(userId);
      
      if (!items || items.length === 0) {
        return { transactions: [], accounts: [] };
      }

      const allTransactions = [];
      const allAccounts = [];

      for (const item of items) {
        try {
          const tokenData = await plaidRepository.getAccessToken(item.item_id);
          
          if (!tokenData) {
            continue;
          }

          const response = await this.client.transactionsGet({
            access_token: tokenData.accessToken,
            start_date: startDate,
            end_date: endDate,
          });

          allTransactions.push(...response.data.transactions);
          allAccounts.push(...response.data.accounts);
        } catch (error) {
          logger.error(`Error fetching transactions for item ${item.item_id}:`, error);
        }
      }

      return {
        transactions: allTransactions,
        accounts: allAccounts,
      };
    } catch (error) {
      logger.error('Error getting transactions:', error);
      throw error;
    }
  }

  /**
   * Remove Plaid item
   */
  async removeItem(userId, itemId) {
    try {
      const tokenData = await plaidRepository.getAccessToken(itemId);
      
      if (!tokenData) {
        throw new Error('Item not found');
      }

      if (tokenData.userId !== userId) {
        throw new Error('Unauthorized');
      }

      // Remove item from Plaid
      await this.client.itemRemove({
        access_token: tokenData.accessToken,
      });

      // Remove from database
      await plaidRepository.deleteItem(itemId, userId);

      logger.info(`Item ${itemId} removed for user ${userId}`);

      return { success: true };
    } catch (error) {
      logger.error('Error removing item:', error);
      throw error;
    }
  }
}

module.exports = new PlaidService();

