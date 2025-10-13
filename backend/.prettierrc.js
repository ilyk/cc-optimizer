/**
 * Prettier Configuration
 * Works alongside ESLint for formatting
 */

module.exports = {
  // Configurable formatting options
  printWidth: parseInt(process.env.PRETTIER_PRINT_WIDTH || '100'),
  tabWidth: parseInt(process.env.PRETTIER_TAB_WIDTH || '2'),
  useTabs: process.env.PRETTIER_USE_TABS === 'true',
  semi: process.env.PRETTIER_SEMI !== 'false',
  singleQuote: process.env.PRETTIER_SINGLE_QUOTE !== 'false',
  quoteProps: 'as-needed',
  jsxSingleQuote: false,
  trailingComma: process.env.PRETTIER_TRAILING_COMMA || 'es5',
  bracketSpacing: true,
  bracketSameLine: false,
  arrowParens: 'always',
  endOfLine: 'lf',
  
  // File-specific overrides
  overrides: [
    {
      files: '*.json',
      options: {
        printWidth: 80,
      },
    },
    {
      files: '*.md',
      options: {
        printWidth: 80,
        proseWrap: 'always',
      },
    },
  ],
};

