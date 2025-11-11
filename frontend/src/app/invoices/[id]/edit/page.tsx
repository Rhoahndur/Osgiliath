'use client';

import React, { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { Header } from '@/components/layout/Header';
import { Button } from '@/components/shared/Button';
import { Input, Select } from '@/components/shared/Input';
import { invoiceService } from '@/services/invoiceService';
import { customerService } from '@/services/customerService';
import { Customer } from '@/models/Customer';
import { Invoice, InvoiceStatus, CreateLineItemRequest } from '@/models/Invoice';
import { formatCurrency, normalizeNumberInput } from '@/utils/format';

interface LineItemForm extends CreateLineItemRequest {
  id?: number;
  isNew?: boolean;
}

export default function EditInvoicePage() {
  const router = useRouter();
  const params = useParams();
  const invoiceId = params.id as string;

  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [lineItems, setLineItems] = useState<LineItemForm[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [formData, setFormData] = useState({
    customerId: '',
    issueDate: '',
    dueDate: ''
  });

  useEffect(() => {
    loadInvoice();
    loadCustomers();
  }, [invoiceId]);

  const loadInvoice = async () => {
    try {
      const data = await invoiceService.getInvoice(invoiceId);

      // Check if invoice is editable
      if (data.status !== InvoiceStatus.DRAFT) {
        setError('Only draft invoices can be edited');
        return;
      }

      setInvoice(data);
      setFormData({
        customerId: data.customerId,
        issueDate: data.issueDate,
        dueDate: data.dueDate
      });

      // Convert existing line items to form format
      setLineItems(data.lineItems.map(item => ({
        id: item.id,
        description: item.description,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        isNew: false
      })));
    } catch (err) {
      console.error('Failed to load invoice:', err);
      setError('Failed to load invoice');
    }
  };

  const loadCustomers = async () => {
    try {
      const response = await customerService.getCustomers(0, 100);
      setCustomers(response.content || []);
    } catch (err) {
      console.error('Failed to load customers:', err);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleAddLineItem = () => {
    setLineItems([
      ...lineItems,
      {
        description: '',
        quantity: 1,
        unitPrice: 0,
        isNew: true
      }
    ]);
  };

  const handleLineItemChange = (index: number, field: keyof CreateLineItemRequest, value: string | number) => {
    const updatedItems = [...lineItems];
    updatedItems[index] = { ...updatedItems[index], [field]: value };
    setLineItems(updatedItems);
  };

  const handleRemoveLineItem = async (index: number) => {
    const item = lineItems[index];

    // If it's an existing line item, delete it via API
    if (!item.isNew && item.id) {
      try {
        await invoiceService.deleteLineItem(invoiceId, item.id);
      } catch (err) {
        console.error('Failed to delete line item:', err);
        setError('Failed to delete line item');
        return;
      }
    }

    // Remove from local state
    setLineItems(lineItems.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // Update basic invoice fields
      await invoiceService.updateInvoice(invoiceId, {
        customerId: formData.customerId,
        issueDate: formData.issueDate,
        dueDate: formData.dueDate
      });

      // Add new line items
      for (const item of lineItems) {
        if (item.isNew) {
          await invoiceService.addLineItem(invoiceId, {
            description: item.description,
            quantity: item.quantity,
            unitPrice: item.unitPrice
          });
        }
      }

      // Redirect back to invoice detail page
      router.push(`/invoices/${invoiceId}`);
    } catch (err) {
      console.error('Failed to update invoice:', err);
      setError('Failed to update invoice');
    } finally {
      setLoading(false);
    }
  };

  const calculateTotal = () => {
    const subtotal = lineItems.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0);
    // Note: Tax rate is fixed at 10% in backend
    const taxAmount = subtotal * 0.10;
    return subtotal + taxAmount;
  };

  if (!invoice && !error) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Loading invoice...</p>
      </div>
    );
  }

  if (error && !invoice) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600">{error}</p>
        <Button variant="secondary" onClick={() => router.push('/invoices')} className="mt-4">
          Back to Invoices
        </Button>
      </div>
    );
  }

  return (
    <div>
      <Header
        title={`Edit Invoice ${invoice?.invoiceNumber}`}
        subtitle="Update invoice details (DRAFT only)"
      />

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
                required
              />

              <Input
                label="Due Date *"
                type="date"
                name="dueDate"
                value={formData.dueDate}
                onChange={handleChange}
                required
              />

              <div className="flex items-end">
                <p className="text-sm text-gray-600">
                  Tax Rate: 10% (fixed)
                </p>
              </div>
            </div>

            {/* Line Items */}
            <div className="border-t pt-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-medium text-gray-900">Line Items</h3>
                <Button type="button" variant="secondary" onClick={handleAddLineItem}>
                  Add Line Item
                </Button>
              </div>

              <div className="space-y-4">
                {lineItems.map((item, index) => (
                  <div key={index} className="grid grid-cols-12 gap-4 items-end">
                    <div className="col-span-5">
                      <Input
                        label="Description"
                        value={item.description}
                        onChange={(e) => handleLineItemChange(index, 'description', e.target.value)}
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
                          const val = normalized === '' ? 0 : parseFloat(normalized);
                          handleLineItemChange(index, 'quantity', isNaN(val) ? 0 : val);
                        }}
                        min="0"
                        step="0.01"
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
                    <div className="col-span-1">
                      <Button
                        type="button"
                        variant="danger"
                        size="sm"
                        onClick={() => handleRemoveLineItem(index)}
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
                      <span className="text-sm text-gray-600">Tax (10%):</span>
                      <span className="text-sm font-medium">
                        ${formatCurrency(lineItems.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0) * 0.10)}
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
                onClick={() => router.push(`/invoices/${invoiceId}`)}
              >
                Cancel
              </Button>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? 'Saving...' : 'Save Changes'}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
