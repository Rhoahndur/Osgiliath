import apiClient from './apiClient';
import {
  MonthlyRevenueDto,
  TopCustomerDto,
  InvoiceStatusBreakdown
} from '@/models/Analytics';

export const analyticsService = {
  /**
   * Get revenue over time
   * @param months Number of months to include (default: 12)
   */
  async getRevenueOverTime(months: number = 12): Promise<MonthlyRevenueDto[]> {
    const response = await apiClient.get<MonthlyRevenueDto[]>('/analytics/revenue-over-time', {
      params: { months }
    });
    return response.data;
  },

  /**
   * Get invoice status breakdown
   * Returns count of invoices by status
   */
  async getStatusBreakdown(): Promise<InvoiceStatusBreakdown> {
    const response = await apiClient.get<InvoiceStatusBreakdown>('/analytics/status-breakdown');
    return response.data;
  },

  /**
   * Get top customers by revenue
   * @param limit Number of top customers to return (default: 10)
   */
  async getTopCustomers(limit: number = 10): Promise<TopCustomerDto[]> {
    const response = await apiClient.get<TopCustomerDto[]>('/analytics/top-customers', {
      params: { limit }
    });
    return response.data;
  }
};
