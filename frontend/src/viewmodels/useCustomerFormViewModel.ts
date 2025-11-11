'use client';

import { useState, useEffect } from 'react';
import { customerService } from '@/services/customerService';
import { Customer, CreateCustomerRequest, UpdateCustomerRequest } from '@/models/Customer';

export const useCustomerFormViewModel = (customerId?: string) => {
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (customerId) {
      loadCustomer();
    }
  }, [customerId]);

  const loadCustomer = async () => {
    if (!customerId) return;

    try {
      setLoading(true);
      setError(null);
      const data = await customerService.getCustomer(customerId);
      setCustomer(data);
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to load customer';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const validateForm = (data: CreateCustomerRequest): boolean => {
    const errors: Record<string, string> = {};

    if (!data.name || data.name.trim().length === 0) {
      errors.name = 'Name is required';
    }

    if (!data.email || data.email.trim().length === 0) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email)) {
      errors.email = 'Invalid email format';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const createCustomer = async (data: CreateCustomerRequest): Promise<Customer> => {
    if (!validateForm(data)) {
      throw new Error('Validation failed');
    }

    try {
      setLoading(true);
      setError(null);
      const newCustomer = await customerService.createCustomer(data);
      return newCustomer;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to create customer';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const updateCustomer = async (data: UpdateCustomerRequest): Promise<Customer> => {
    if (!customerId) {
      throw new Error('Customer ID is required for update');
    }

    try {
      setLoading(true);
      setError(null);
      const updatedCustomer = await customerService.updateCustomer(customerId, data);
      setCustomer(updatedCustomer);
      return updatedCustomer;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to update customer';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return {
    customer,
    loading,
    error,
    validationErrors,
    createCustomer,
    updateCustomer,
    refreshCustomer: loadCustomer
  };
};
