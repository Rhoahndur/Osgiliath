import apiClient from './apiClient';
import { Payment, CreatePaymentRequest } from '@/models/Payment';

export const paymentService = {
  async recordPayment(invoiceId: string, payment: CreatePaymentRequest): Promise<Payment> {
    const response = await apiClient.post<Payment>(
      `/invoices/${invoiceId}/payments`,
      payment
    );
    return response.data;
  },

  async getPayments(invoiceId: string): Promise<Payment[]> {
    const response = await apiClient.get<Payment[]>(`/invoices/${invoiceId}/payments`);
    return response.data;
  }
};
