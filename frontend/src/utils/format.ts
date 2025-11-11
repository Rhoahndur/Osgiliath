/**
 * Format a number as currency with comma separators
 * @param value - The number to format
 * @param decimals - Number of decimal places (default: 2)
 * @returns Formatted string like "1,234.56"
 */
export function formatCurrency(value: number | undefined | null, decimals: number = 2): string {
  if (value === undefined || value === null || isNaN(value)) {
    return '0.00';
  }
  return value.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  });
}

/**
 * Remove leading zeros from a numeric string
 * @param value - The string value to normalize
 * @returns Normalized string without leading zeros
 */
export function removeLeadingZeros(value: string): string {
  if (!value || value === '0') return value;

  // Handle decimal numbers
  if (value.includes('.')) {
    const [intPart, decPart] = value.split('.');
    const normalizedInt = intPart.replace(/^0+/, '') || '0';
    return `${normalizedInt}.${decPart}`;
  }

  // Handle whole numbers
  return value.replace(/^0+/, '') || '0';
}

/**
 * Normalize a number input value
 * Removes leading zeros and ensures valid number format
 * @param value - The input value to normalize
 * @returns Normalized value
 */
export function normalizeNumberInput(value: string): string {
  // Remove any non-numeric characters except decimal point
  let cleaned = value.replace(/[^\d.]/g, '');

  // Ensure only one decimal point
  const parts = cleaned.split('.');
  if (parts.length > 2) {
    cleaned = parts[0] + '.' + parts.slice(1).join('');
  }

  // Remove leading zeros
  return removeLeadingZeros(cleaned);
}
