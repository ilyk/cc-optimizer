/**
 * ESLint Configuration for Node.js Backend
 * 
 * Enforces code quality and consistency
 * Configurable via environment or override files
 */

module.exports = {
  env: {
    node: true,
    es6: true,
    jest: true,
  },
  
  extends: [
    'eslint:recommended',
    // Optional: uncomment if using these configs
    // 'plugin:security/recommended',
    // 'plugin:node/recommended',
  ],
  
  parserOptions: {
    ecmaVersion: 2020,
    sourceType: 'script',
  },
  
  plugins: [
    // 'security',  // npm install --save-dev eslint-plugin-security
    // 'node',      // npm install --save-dev eslint-plugin-node
  ],
  
  rules: {
    // =========================================================================
    // ERROR PREVENTION
    // =========================================================================
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'warn',
    'no-unused-vars': ['error', { 
      argsIgnorePattern: '^_',
      varsIgnorePattern: '^_',
    }],
    'no-var': 'error',
    'prefer-const': 'error',
    'no-use-before-define': ['error', { functions: false, classes: true }],
    
    // =========================================================================
    // BEST PRACTICES
    // =========================================================================
    'eqeqeq': ['error', 'always', { null: 'ignore' }],
    'curly': ['error', 'all'],
    'no-eval': 'error',
    'no-implied-eval': 'error',
    'no-new-func': 'error',
    'no-return-await': 'error',
    'require-await': 'warn',
    'no-throw-literal': 'error',
    'prefer-promise-reject-errors': 'error',
    
    // =========================================================================
    // ASYNC/AWAIT
    // =========================================================================
    'no-async-promise-executor': 'error',
    'no-await-in-loop': 'warn',
    // 'no-promise-executor-return': 'error',  // Not available in older ESLint
    
    // =========================================================================
    // CODE STYLE (configurable)
    // =========================================================================
    'indent': ['error', 2, { 
      SwitchCase: 1,
      VariableDeclarator: 1,
      outerIIFEBody: 1,
    }],
    'linebreak-style': ['error', 'unix'],
    'quotes': ['error', 'single', { avoidEscape: true }],
    'semi': ['error', 'always'],
    'comma-dangle': ['error', 'only-multiline'],
    'comma-spacing': ['error', { before: false, after: true }],
    'object-curly-spacing': ['error', 'always'],
    'array-bracket-spacing': ['error', 'never'],
    'keyword-spacing': ['error', { before: true, after: true }],
    'space-before-blocks': 'error',
    'space-before-function-paren': ['error', {
      anonymous: 'always',
      named: 'never',
      asyncArrow: 'always',
    }],
    'arrow-spacing': ['error', { before: true, after: true }],
    'brace-style': ['error', '1tbs', { allowSingleLine: true }],
    
    // =========================================================================
    // NAMING CONVENTIONS
    // =========================================================================
    'camelcase': ['error', { 
      properties: 'never',
      ignoreDestructuring: true,
      allow: [
        'access_token',
        'public_token',
        'item_id',
        'account_id',
        'webhook_type',
        'webhook_code',
        'credit_limit',
        'statement_date',
        'due_date',
        'auto_pay_enabled',
        'auto_pay_amount',
        'password_hash',
      ],
    }],
    
    // =========================================================================
    // COMPLEXITY
    // =========================================================================
    'max-depth': ['warn', 4],
    'max-lines': ['warn', { max: 500, skipBlankLines: true, skipComments: true }],
    'max-lines-per-function': ['warn', { max: 100, skipBlankLines: true, skipComments: true }],
    'max-params': ['warn', 5],
    'complexity': ['warn', 15],
    
    // =========================================================================
    // SECURITY (if plugin enabled)
    // =========================================================================
    // 'security/detect-object-injection': 'off',  // Too many false positives
    // 'security/detect-non-literal-regexp': 'warn',
    // 'security/detect-unsafe-regex': 'error',
    
    // =========================================================================
    // NODE.JS SPECIFIC (if plugin enabled)
    // =========================================================================
    // 'node/no-unsupported-features/es-syntax': 'off',  // We use modern syntax
    // 'node/no-missing-require': 'error',
    // 'node/no-unpublished-require': 'off',
  },
  
  overrides: [
    // Test files
    {
      files: ['**/*.test.js', '**/*.spec.js', '**/test/**/*.js'],
      env: {
        jest: true,
      },
      rules: {
        'max-lines-per-function': 'off',
        'no-unused-expressions': 'off',
      },
    },
    
    // Config files
    {
      files: ['*.config.js', '.eslintrc.js'],
      env: {
        node: true,
      },
      rules: {
        'no-console': 'off',
      },
    },
  ],
};

