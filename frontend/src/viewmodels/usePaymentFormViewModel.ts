'use client';

import { useState, useEffect } from 'react';
import { paymentService } from '@/services/paymentService';
import { invoiceService } from '@/services/invoiceService';
import { Payment, CreatePaymentRequest } from '@/models/Payment';
import { Invoice } from '@/models/Invoice';

export const usePaymentFormViewModel = (invoiceId: string) => {
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    loadInvoiceAndPayments();
  }, [invoiceId]);

  const loadInvoiceAndPayments = async () => {
    try {
      setLoading(true);
      setError(null);
      const [invoiceData, paymentsData] = await Promise.all([
        invoiceService.getInvoice(invoiceId),
        paymentService.getPayments(invoiceId)
      ]);
      setInvoice(invoiceData);
      setPayments(paymentsData);
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to load invoice data';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const validatePayment = (data: CreatePaymentRequest): boolean => {
    const errors: Record<string, string> = {};

    if (!data.amount || data.amount <= 0) {
      errors.amount = 'Amount must be greater than 0';
    }

    if (invoice && data.amount > invoice.balanceDue) {
      errors.amount = `Amount cannot exceed invoice balance of ${invoice.balanceDue}`;
    }

    if (!data.paymentDate) {
      errors.paymentDate = 'Payment date is required';
    }

    if (!data.paymentMethod) {
      errors.paymentMethod = 'Payment method is required';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const recordPayment = async (data: CreatePaymentRequest): Promise<Payment> => {
    if (!validatePayment(data)) {
      throw new Error('Validation failed');
    }

    try {
      setLoading(true);
      setError(null);
      const payment = await paymentService.recordPayment(invoiceId, data);
      await loadInvoiceAndPayments();
      return payment;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to record payment';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return {
    invoice,
    payments,
    loading,
    error,
    validationErrors,
    recordPayment,
    refreshData: loadInvoiceAndPayments
  };
};
