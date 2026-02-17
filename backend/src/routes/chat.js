const express = require('express');
const router = express.Router();
const { authMiddleware } = require('../middleware/auth');
const { apiLimiter } = require('../middleware/rateLimiter');
const { asyncHandler } = require('../middleware/errorHandler');
const llmService = require('../services/llmService');
const plaidService = require('../services/plaidService');
const logger = require('../utils/logger');

/**
 * POST /api/v1/chat
 * Send a chat message and get AI response with card context
 */
router.post('/', authMiddleware, apiLimiter, asyncHandler(async (req, res) => {
  const { message } = req.body;

  if (!message || typeof message !== 'string' || message.trim().length === 0) {
    return res.status(400).json({ error: 'Message is required' });
  }

  if (message.length > 2000) {
    return res.status(400).json({ error: 'Message too long (max 2000 characters)' });
  }

  try {
    // Get user's card data for context
    let cardContext = '';
    try {
      const accounts = await plaidService.getAccounts(req.userId);
      if (accounts && accounts.length > 0) {
        cardContext = buildCardContext(accounts);
      }
    } catch (cardError) {
      logger.warn('Could not fetch card data for chat context:', cardError.message);
      // Continue without card context
    }

    // Build system prompt with card context
    const systemPrompt = buildSystemPrompt(cardContext);

    // Get AI response
    let responseText;

    if (!llmService.isAvailable()) {
      // Fallback to rule-based response if no LLM available
      responseText = generateFallbackResponse(message.trim());
    } else if (llmService.provider === 'openai') {
      const response = await llmService.openaiClient.chat.completions.create({
        model: process.env.OPENAI_CHAT_MODEL || process.env.OPENAI_MODEL || 'gpt-4o-mini',
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: message.trim() }
        ],
        max_completion_tokens: 500,
      });
      responseText = response.choices[0]?.message?.content?.trim() || 'I apologize, I could not generate a response.';
    } else if (llmService.provider === 'anthropic') {
      const response = await llmService.anthropicClient.messages.create({
        model: process.env.ANTHROPIC_CHAT_MODEL || process.env.ANTHROPIC_MODEL || 'claude-3-haiku-20240307',
        max_tokens: 500,
        system: systemPrompt,
        messages: [
          { role: 'user', content: message.trim() }
        ]
      });
      responseText = response.content[0]?.text?.trim() || 'I apologize, I could not generate a response.';
    }

    res.json({
      message: responseText,
      actions: extractActions(responseText)
    });

  } catch (error) {
    logger.error('Chat endpoint error:', error);
    res.status(500).json({ error: 'Failed to process chat message' });
  }
}));

/**
 * Build card context string from Plaid accounts
 */
function buildCardContext(accounts) {
  const lines = accounts.map(account => {
    const balance = account.balances?.current || 0;
    const limit = account.balances?.limit || 0;
    const utilization = limit > 0 ? ((balance / limit) * 100).toFixed(1) : 'N/A';
    const lastStatement = account.liability?.lastStatementDate || 'Unknown';
    const dueDate = account.liability?.nextPaymentDueDate || 'Unknown';
    const minPayment = account.liability?.minimumPaymentAmount || 0;

    return `- ${account.name}: Balance $${balance.toFixed(2)}, Limit $${limit.toFixed(2)}, Utilization ${utilization}%, Statement closes: ${lastStatement}, Due: ${dueDate}, Min payment: $${minPayment.toFixed(2)}`;
  });

  return `\nUser's Credit Cards:\n${lines.join('\n')}\n`;
}

/**
 * Build system prompt with optional card context
 */
function buildSystemPrompt(cardContext) {
  return `You are Timekeeper, an AI assistant for a credit card payment optimizer app called "Credit Cycle". Your job is to help users understand when to pay their credit cards to minimize reported utilization on their credit reports.

Key knowledge:
- Credit utilization reported to bureaus is based on the STATEMENT BALANCE, not the current balance
- Paying BEFORE the statement closes reduces the reported utilization
- Ideal utilization is 1-9% (not 0%, as having some activity is beneficial)
- Users should pay down cards a few days before the statement close date to allow processing time
- Different banks have different processing times (Amex: same day, Chase: 2 days, etc.)

Your personality:
- Concise and helpful (keep responses under 150 words)
- Use plain language, no jargon
- Give specific actionable advice when you have card data
- If you don't have card data, guide users to connect their cards
${cardContext}
When giving advice, reference specific card names and numbers. Be specific about dates and amounts.`;
}

/**
 * Generate fallback response when LLM is unavailable
 */
function generateFallbackResponse(message) {
  const lower = message.toLowerCase();

  if (lower.includes('when') && lower.includes('pay')) {
    return 'To minimize utilization, pay your cards 2-3 days before the statement close date. This gives the bank time to process the payment before reporting to credit bureaus.';
  }
  if (lower.includes('utilization')) {
    return 'Credit utilization is your balance divided by your credit limit. Aim for 1-9% when your statement closes. Pay down the balance a few days before the close date.';
  }
  if (lower.includes('help') || lower.includes('what can')) {
    return 'I can help with:\n\u2022 When to pay each card\n\u2022 How to minimize utilization\n\u2022 Understanding statement dates\n\u2022 Payment prioritization\n\nConnect your cards in the Home tab for personalized advice!';
  }

  return 'I\'m here to help you optimize your credit card payments. Try asking me "when should I pay?" or "how does utilization work?" For personalized advice, make sure your cards are connected.';
}

/**
 * Extract actionable items from AI response (placeholder for future use)
 */
function extractActions(responseText) {
  // Future: parse response for actionable suggestions
  // For now, return null
  return null;
}

module.exports = router;
