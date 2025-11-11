import apiClient from './apiClient';
import {
  Customer,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  CustomerListResponse
} from '@/models/Customer';

export const customerService = {
  async getCustomers(page: number = 0, pageSize: number = 10, search?: string): Promise<CustomerListResponse> {
    const response = await apiClient.get<CustomerListResponse>('/customers', {
      params: {
        page,
        size: pageSize,
        ...(search && { search })
      }
    });
    return response.data;
  },

  async getCustomer(id: string): Promise<Customer> {
    const response = await apiClient.get<Customer>(`/customers/${id}`);
    return response.data;
  },

  async createCustomer(customer: CreateCustomerRequest): Promise<Customer> {
    const response = await apiClient.post<Customer>('/customers', customer);
    return response.data;
  },

  async updateCustomer(id: string, customer: UpdateCustomerRequest): Promise<Customer> {
    const response = await apiClient.put<Customer>(`/customers/${id}`, customer);
    return response.data;
  },

  async deleteCustomer(id: string): Promise<void> {
    await apiClient.delete(`/customers/${id}`);
  }
};
