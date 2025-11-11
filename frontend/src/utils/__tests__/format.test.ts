import { formatCurrency, normalizeNumberInput, removeLeadingZeros } from '../format';

describe('Format Utilities', () => {
  describe('formatCurrency', () => {
    it('formats positive numbers correctly', () => {
      expect(formatCurrency(1234.56)).toBe('1,234.56');
    });

    it('formats zero correctly', () => {
      expect(formatCurrency(0)).toBe('0.00');
    });

    it('formats negative numbers correctly', () => {
      expect(formatCurrency(-1234.56)).toBe('-1,234.56');
    });

    it('rounds to two decimal places', () => {
      expect(formatCurrency(1234.567)).toBe('1,234.57');
      expect(formatCurrency(1234.561)).toBe('1,234.56');
    });

    it('adds trailing zeros when needed', () => {
      expect(formatCurrency(1234)).toBe('1,234.00');
      expect(formatCurrency(1234.5)).toBe('1,234.50');
    });

    it('handles large numbers', () => {
      expect(formatCurrency(1234567.89)).toBe('1,234,567.89');
    });

    it('handles null and undefined', () => {
      expect(formatCurrency(null)).toBe('0.00');
      expect(formatCurrency(undefined)).toBe('0.00');
    });
  });

  describe('removeLeadingZeros', () => {
    it('removes leading zeros from whole numbers', () => {
      expect(removeLeadingZeros('0123')).toBe('123');
      expect(removeLeadingZeros('00456')).toBe('456');
    });

    it('preserves single zero', () => {
      expect(removeLeadingZeros('0')).toBe('0');
    });

    it('handles decimal numbers', () => {
      expect(removeLeadingZeros('012.34')).toBe('12.34');
      expect(removeLeadingZeros('0.56')).toBe('0.56');
    });

    it('handles empty string', () => {
      expect(removeLeadingZeros('')).toBe('');
    });
  });

  describe('normalizeNumberInput', () => {
    it('removes non-numeric characters', () => {
      expect(normalizeNumberInput('abc123def')).toBe('123');
    });

    it('allows single decimal point', () => {
      expect(normalizeNumberInput('12.34')).toBe('12.34');
    });

    it('removes multiple decimal points', () => {
      expect(normalizeNumberInput('12.3.4')).toBe('12.34');
    });

    it('removes leading zeros', () => {
      expect(normalizeNumberInput('0123')).toBe('123');
      expect(normalizeNumberInput('012.34')).toBe('12.34');
    });
  });
});
