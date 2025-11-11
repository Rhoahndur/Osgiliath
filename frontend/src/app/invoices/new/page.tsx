'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Header } from '@/components/layout/Header';
import { Button } from '@/components/shared/Button';
import { Input, Select } from '@/components/shared/Input';
import { useInvoiceFormViewModel } from '@/viewmodels/useInvoiceFormViewModel';
import { customerService } from '@/services/customerService';
import { CreateInvoiceRequest, CreateLineItemRequest } from '@/models/Invoice';
import { Customer } from '@/models/Customer';
import { formatCurrency, normalizeNumberInput } from '@/utils/format';

export default function NewInvoicePage() {
  const router = useRouter();
  const { createInvoice, loading, error, validationErrors, lineItems, addLineItem, removeLineItem, updateLineItem } = useInvoiceFormViewModel();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [formData, setFormData] = useState({
    customerId: '',
    issueDate: new Date().toISOString().split('T')[0],
    dueDate: '',
    taxRate: '0'
  });

  useEffect(() => {
    loadCustomers();
  }, []);

  const loadCustomers = async () => {
    try {
      // Spring Data uses 0-based page numbers
      const response = await customerService.getCustomers(0, 100);
      // Handle Spring Page response structure (content, totalElements)
      setCustomers(response.content || []);
    } catch (err) {
      console.error('Failed to load customers:', err);
      setCustomers([]); // Set empty array on error
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleAddLineItem = () => {
    addLineItem({
      description: '',
      quantity: 1,
      unitPrice: 0
    });
  };

  const handleLineItemChange = (index: number, field: keyof CreateLineItemRequest, value: string | number) => {
    const updatedItem = { ...lineItems[index], [field]: value };
    updateLineItem(index, updatedItem);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const invoiceData: CreateInvoiceRequest = {
        customerId: formData.customerId, // Keep as string (UUID)
        issueDate: formData.issueDate,
        dueDate: formData.dueDate,
        taxRate: parseFloat(formData.taxRate),
        lineItems: lineItems
      };

      await createInvoice(invoiceData);
      // Redirect to invoices list page since detail page doesn't exist yet
      router.push('/invoices');
    } catch (err) {
      console.error('Failed to create invoice:', err);
    }
  };

  const calculateTotal = () => {
    const subtotal = lineItems.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0);
    const taxAmount = subtotal * (parseFloat(formData.taxRate) / 100);
    return subtotal + taxAmount;
  };

  return (
    <div>
      <Header title="New Invoice" subtitle="Create a new invoice" />

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white shadow rounded-lg p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="rounded-md bg-red-50 p-4">
                <p className="text-sm text-red-800">{error}</p>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Select
                label="Customer *"
                name="customerId"
                value={formData.customerId}
                onChange={handleChange}
                error={validationErrors.customerId}
                options={[
                  { value: '', label: 'Select a customer' },
                  ...customers.map(c => ({ value: c.id.toString(), label: c.name }))
                ]}
                required
              />

              <Input
                label="Issue Date *"
                type="date"
                name="issueDate"
                value={formData.issueDate}
                onChange={handleChange}
                error={validationErrors.issueDate}
                required
              />

              <Input
                label="Due Date *"
                type="date"
                name="dueDate"
                value={formData.dueDate}
                onChange={handleChange}
                error={validationErrors.dueDate}
                required
              />

              <Input
                label="Tax Rate (%)"
                type="number"
                name="taxRate"
                value={formData.taxRate}
                onChange={handleChange}
                step="0.01"
                min="0"
                max="100"
              />
            </div>

            {/* Line Items */}
            <div className="border-t pt-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-medium text-gray-900">Line Items</h3>
                <Button type="button" variant="secondary" onClick={handleAddLineItem}>
                  Add Line Item
                </Button>
              </div>

              {validationErrors.lineItems && (
                <p className="text-sm text-red-600 mb-4">{validationErrors.lineItems}</p>
              )}

              <div className="space-y-4">
                {lineItems.map((item, index) => (
                  <div key={index} className="grid grid-cols-12 gap-4 items-end">
                    <div className="col-span-5">
                      <Input
                        label="Description"
                        value={item.description}
                        onChange={(e) => handleLineItemChange(index, 'description', e.target.value)}
                        error={validationErrors[`lineItem_${index}_description`]}
                        required
                      />
                    </div>
                    <div className="col-span-2">
                      <Input
                        label="Quantity"
                        type="number"
                        value={item.quantity}
                        onChange={(e) => {
                          const normalized = normalizeNumberInput(e.target.value);
                          const val = normalized === '' ? 0 : parseInt(normalized);
                          handleLineItemChange(index, 'quantity', isNaN(val) ? 0 : val);
                        }}
                        error={validationErrors[`lineItem_${index}_quantity`]}
                        min="0"
                        step="1"
                        required
                      />
                    </div>
                    <div className="col-span-2">
                      <Input
                        label="Unit Price"
                        type="number"
                        value={item.unitPrice}
                        onChange={(e) => {
                          const normalized = normalizeNumberInput(e.target.value);
                          const val = normalized === '' ? 0 : parseFloat(normalized);
                          handleLineItemChange(index, 'unitPrice', isNaN(val) ? 0 : val);
                        }}
                        error={validationErrors[`lineItem_${index}_unitPrice`]}
                        min="0"
                        step="0.01"
                        required
                      />
                    </div>
                    <div className="col-span-2">
                      <label className="block text-sm font-medium text-gray-700 mb-1">Total</label>
                      <p className="text-sm font-semibold text-gray-900 mt-2">
                        ${formatCurrency(item.quantity * item.unitPrice)}
                      </p>
                    </div>
                    <div className="col-span-1 pr-2">
                      <Button
                        type="button"
                        variant="danger"
                        size="sm"
                        onClick={() => removeLineItem(index)}
                      >
                        Remove
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Total */}
            {lineItems.length > 0 && (
              <div className="border-t pt-4">
                <div className="flex justify-end">
                  <div className="w-64">
                    <div className="flex justify-between mb-2">
                      <span className="text-sm text-gray-600">Subtotal:</span>
                      <span className="text-sm font-medium">
                        ${formatCurrency(lineItems.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0))}
                      </span>
                    </div>
                    <div className="flex justify-between mb-2">
                      <span className="text-sm text-gray-600">Tax ({formData.taxRate}%):</span>
                      <span className="text-sm font-medium">
                        ${formatCurrency(lineItems.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0) * (parseFloat(formData.taxRate) / 100))}
                      </span>
                    </div>
                    <div className="flex justify-between border-t pt-2">
                      <span className="text-base font-semibold">Total:</span>
                      <span className="text-base font-bold">
                        ${formatCurrency(calculateTotal())}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            <div className="flex justify-end space-x-3 border-t pt-6">
              <Button
                type="button"
                variant="secondary"
                onClick={() => router.push('/invoices')}
              >
                Cancel
              </Button>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? 'Creating...' : 'Create Invoice'}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
