const OpenAI = require('openai');
const Anthropic = require('@anthropic-ai/sdk');
const logger = require('../utils/logger');

/**
 * LLM Service for intelligent transaction analysis
 * Supports multiple providers: OpenAI, Anthropic (Claude)
 */
class LLMService {
  constructor() {
    this.provider = process.env.LLM_PROVIDER || 'openai'; // or 'anthropic' or 'none'
    this.openaiClient = null;
    this.anthropicClient = null;
    this.initialize();
  }

  initialize() {
    if (this.provider === 'none') {
      logger.info('LLM provider disabled - using pattern matching only');
      return;
    }

    try {
      if (this.provider === 'openai' && process.env.OPENAI_API_KEY) {
        this.openaiClient = new OpenAI({
          apiKey: process.env.OPENAI_API_KEY,
        });
        logger.info('LLM Service initialized with OpenAI');
      } else if (this.provider === 'anthropic' && process.env.ANTHROPIC_API_KEY) {
        this.anthropicClient = new Anthropic({
          apiKey: process.env.ANTHROPIC_API_KEY,
        });
        logger.info('LLM Service initialized with Anthropic (Claude)');
      } else {
        logger.warn(`LLM provider '${this.provider}' configured but no API key found - falling back to pattern matching`);
        this.provider = 'none';
      }
    } catch (error) {
      logger.error('Failed to initialize LLM service:', error);
      this.provider = 'none';
    }
  }

  /**
   * Analyze transactions using LLM to find statement closing dates
   * Much smarter than string matching - understands context
   */
  async analyzeTransactionsWithLLM(transactions, accountInfo) {
    if (this.provider === 'none' || transactions.length < 10) {
      return null; // Not available or not enough data
    }

    try {
      // Prepare transaction sample (last 3 months to reduce token usage)
      const recentTransactions = transactions
        .slice(0, Math.min(100, transactions.length))
        .map(t => ({
          date: t.date,
          name: t.name,
          amount: t.amount,
          category: t.category ? t.category.join(', ') : 'uncategorized'
        }));

      const prompt = this.buildAnalysisPrompt(recentTransactions, accountInfo);

      let statementDay = null;

      if (this.provider === 'openai') {
        statementDay = await this.analyzeWithOpenAI(prompt);
      } else if (this.provider === 'anthropic') {
        statementDay = await this.analyzeWithAnthropic(prompt);
      }

      if (statementDay && statementDay >= 1 && statementDay <= 31) {
        logger.info(`[LLM] Detected statement day ${statementDay} for account using ${this.provider}`);
        return statementDay;
      }

      return null;
    } catch (error) {
      logger.error(`LLM analysis failed: ${error.message}`);
      return null; // Fall back to other methods
    }
  }

  /**
   * Build prompt for LLM analysis
   */
  buildAnalysisPrompt(transactions, accountInfo) {
    const transactionList = transactions
      .map((t, i) => `${i + 1}. ${t.date} - ${t.name} - $${t.amount}`)
      .join('\n');

    return `Analyze these credit card transactions to determine the recurring statement closing date (day of month).

Credit Card: ${accountInfo.name || 'Unknown'}
Bank: ${accountInfo.bankName || 'Unknown'}

Recent Transactions:
${transactionList}

Instructions:
1. Look for patterns that indicate when the statement closes each month
2. Common indicators:
   - Statement fees or charges that post on the same day each month
   - Clusters of transactions posting on the same day
   - Monthly recurring charges
   - Balance adjustments
3. Analyze the DAY OF MONTH (1-31) when statements typically close
4. Account for weekend/holiday shifts (if 18th is usually the day but one month shows 20th due to weekend, the real day is 18th)
5. Return ONLY the most likely recurring day of month as a number between 1 and 31
6. If you cannot determine a clear pattern, respond with "UNKNOWN"

Response format: Just the number (e.g., "18") or "UNKNOWN"`;
  }

  /**
   * Analyze using OpenAI
   */
  async analyzeWithOpenAI(prompt) {
    const response = await this.openaiClient.chat.completions.create({
      model: process.env.OPENAI_MODEL || 'gpt-4o-mini', // Fast and cost-effective
      messages: [
        {
          role: 'system',
          content: 'You are a financial analyst expert at identifying credit card statement closing dates from transaction patterns. Respond with ONLY the day number (1-31) or "UNKNOWN".'
        },
        {
          role: 'user',
          content: prompt
        }
      ],
      // Note: Removed temperature:0 as newer models only support default (1)
      max_completion_tokens: 10, // Just need a number (renamed in newer OpenAI models)
    });

    const result = response.choices[0]?.message?.content?.trim();
    
    if (result === 'UNKNOWN' || !result) {
      return null;
    }

    const day = parseInt(result);
    return isNaN(day) ? null : day;
  }

  /**
   * Analyze using Anthropic (Claude)
   */
  async analyzeWithAnthropic(prompt) {
    const response = await this.anthropicClient.messages.create({
      model: process.env.ANTHROPIC_MODEL || 'claude-3-haiku-20240307', // Fast and cost-effective
      max_tokens: 10,
      temperature: 0,
      messages: [
        {
          role: 'user',
          content: prompt
        }
      ],
      system: 'You are a financial analyst expert at identifying credit card statement closing dates from transaction patterns. Respond with ONLY the day number (1-31) or "UNKNOWN".'
    });

    const result = response.content[0]?.text?.trim();
    
    if (result === 'UNKNOWN' || !result) {
      return null;
    }

    const day = parseInt(result);
    return isNaN(day) ? null : day;
  }

  /**
   * Check if LLM is available
   */
  isAvailable() {
    return this.provider !== 'none' && 
           (this.openaiClient !== null || this.anthropicClient !== null);
  }

  /**
   * Get current provider name
   */
  getProvider() {
    return this.provider;
  }
}

module.exports = new LLMService();

