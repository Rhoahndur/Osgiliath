'use client';

import { useState, useEffect } from 'react';
import { invoiceService } from '@/services/invoiceService';
import {
  Invoice,
  CreateInvoiceRequest,
  CreateLineItemRequest,
  UpdateInvoiceRequest,
  LineItem
} from '@/models/Invoice';

export const useInvoiceFormViewModel = (invoiceId?: string) => {
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [lineItems, setLineItems] = useState<CreateLineItemRequest[]>([]);

  useEffect(() => {
    if (invoiceId) {
      loadInvoice();
    }
  }, [invoiceId]);

  const loadInvoice = async () => {
    if (!invoiceId) return;

    try {
      setLoading(true);
      setError(null);
      const data = await invoiceService.getInvoice(invoiceId);
      setInvoice(data);
      setLineItems(data.lineItems.map(item => ({
        description: item.description,
        quantity: item.quantity,
        unitPrice: item.unitPrice
      })));
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to load invoice';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const validateForm = (data: CreateInvoiceRequest): boolean => {
    const errors: Record<string, string> = {};

    if (!data.customerId) {
      errors.customerId = 'Customer is required';
    }

    if (!data.issueDate) {
      errors.issueDate = 'Issue date is required';
    }

    if (!data.dueDate) {
      errors.dueDate = 'Due date is required';
    }

    if (data.lineItems.length === 0) {
      errors.lineItems = 'At least one line item is required';
    }

    data.lineItems.forEach((item, index) => {
      if (!item.description || item.description.trim().length === 0) {
        errors[`lineItem_${index}_description`] = 'Description is required';
      }
      if (!item.quantity || item.quantity <= 0) {
        errors[`lineItem_${index}_quantity`] = 'Quantity must be greater than 0';
      }
      if (!item.unitPrice || item.unitPrice <= 0) {
        errors[`lineItem_${index}_unitPrice`] = 'Unit price must be greater than 0';
      }
    });

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const createInvoice = async (data: CreateInvoiceRequest): Promise<Invoice> => {
    if (!validateForm(data)) {
      throw new Error('Validation failed');
    }

    try {
      setLoading(true);
      setError(null);
      const newInvoice = await invoiceService.createInvoice(data);
      return newInvoice;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to create invoice';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const updateInvoice = async (data: UpdateInvoiceRequest): Promise<Invoice> => {
    if (!invoiceId) {
      throw new Error('Invoice ID is required for update');
    }

    try {
      setLoading(true);
      setError(null);
      const updatedInvoice = await invoiceService.updateInvoice(invoiceId, data);
      setInvoice(updatedInvoice);
      return updatedInvoice;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to update invoice';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const addLineItem = (item: CreateLineItemRequest) => {
    setLineItems([...lineItems, item]);
  };

  const removeLineItem = (index: number) => {
    setLineItems(lineItems.filter((_, i) => i !== index));
  };

  const updateLineItem = (index: number, item: CreateLineItemRequest) => {
    const updated = [...lineItems];
    updated[index] = item;
    setLineItems(updated);
  };

  const addLineItemToExistingInvoice = async (item: CreateLineItemRequest) => {
    if (!invoiceId) {
      throw new Error('Invoice ID is required');
    }

    try {
      setLoading(true);
      setError(null);
      await invoiceService.addLineItem(invoiceId, item);
      await loadInvoice();
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to add line item';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const deleteLineItemFromInvoice = async (lineItemId: number) => {
    if (!invoiceId) {
      throw new Error('Invoice ID is required');
    }

    try {
      setLoading(true);
      setError(null);
      await invoiceService.deleteLineItem(invoiceId, lineItemId);
      await loadInvoice();
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to delete line item';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return {
    invoice,
    loading,
    error,
    validationErrors,
    lineItems,
    createInvoice,
    updateInvoice,
    addLineItem,
    removeLineItem,
    updateLineItem,
    addLineItemToExistingInvoice,
    deleteLineItemFromInvoice,
    refreshInvoice: loadInvoice
  };
};
