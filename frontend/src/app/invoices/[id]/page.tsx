'use client';

import React, { useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { Header } from '@/components/layout/Header';
import { Button } from '@/components/shared/Button';
import { Modal } from '@/components/shared/Modal';
import { Input, Select } from '@/components/shared/Input';
import { useInvoiceFormViewModel } from '@/viewmodels/useInvoiceFormViewModel';
import { usePaymentFormViewModel } from '@/viewmodels/usePaymentFormViewModel';
import { invoiceService } from '@/services/invoiceService';
import { InvoiceStatus } from '@/models/Invoice';
import { PaymentMethod, CreatePaymentRequest } from '@/models/Payment';
import { formatCurrency } from '@/utils/format';

export default function InvoiceDetailPage() {
  const router = useRouter();
  const params = useParams();
  const invoiceId = params.id as string; // UUID from backend

  const { invoice, loading: invoiceLoading, refreshInvoice } = useInvoiceFormViewModel(invoiceId);
  const { payments, recordPayment, loading: paymentLoading } = usePaymentFormViewModel(invoiceId);

  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);

  // Get today's date in YYYY-MM-DD format
  const getTodayDate = () => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const [paymentData, setPaymentData] = useState<CreatePaymentRequest>({
    amount: 0,
    paymentDate: getTodayDate(),
    paymentMethod: PaymentMethod.CASH,
    referenceNumber: ''
  });

  const handleSendInvoice = async () => {
    if (confirm('Are you sure you want to send this invoice?')) {
      try {
        await invoiceService.sendInvoice(invoiceId);
        window.location.reload();
      } catch (err) {
        alert('Failed to send invoice');
      }
    }
  };

  const handleMarkAsPaid = async () => {
    if (confirm('Are you sure you want to mark this invoice as paid? This will set the balance to zero.')) {
      try {
        await invoiceService.markInvoiceAsPaid(invoiceId);
        window.location.reload();
      } catch (err) {
        alert('Failed to mark invoice as paid');
      }
    }
  };

  const handleCancel = async () => {
    if (confirm('Are you sure you want to cancel this invoice? This action cannot be undone.')) {
      try {
        await invoiceService.cancelInvoice(invoiceId);
        window.location.reload();
      } catch (err) {
        alert('Failed to cancel invoice');
      }
    }
  };

  const handleDelete = async () => {
    if (confirm('Are you sure you want to delete this invoice? This action cannot be undone.')) {
      try {
        await invoiceService.deleteInvoice(invoiceId);
        router.push('/invoices');
      } catch (err) {
        alert('Failed to delete invoice');
      }
    }
  };

  const handlePaymentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await recordPayment(paymentData);
      await refreshInvoice(); // Refresh invoice data to update balance and status
      setIsPaymentModalOpen(false);
      setPaymentData({
        amount: 0,
        paymentDate: getTodayDate(),
        paymentMethod: PaymentMethod.CASH,
        referenceNumber: ''
      });
    } catch (err) {
      alert('Failed to record payment');
    }
  };

  const handleExportPdf = async () => {
    if (!invoice) return;
    try {
      await invoiceService.exportInvoiceToPdf(invoiceId, invoice.invoiceNumber);
    } catch (err) {
      alert('Failed to export PDF');
    }
  };

  if (invoiceLoading && !invoice) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Loading invoice...</p>
      </div>
    );
  }

  if (!invoice) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Invoice not found</p>
      </div>
    );
  }

  return (
    <div>
      <Header
        title={`Invoice ${invoice.invoiceNumber}`}
        subtitle={`Customer: ${invoice.customerName}`}
        action={
          <div className="space-x-2">
            <Button variant="secondary" onClick={handleExportPdf}>
              Export PDF
            </Button>
            {invoice.status === InvoiceStatus.DRAFT && (
              <>
                <Button variant="secondary" onClick={() => router.push(`/invoices/${invoiceId}/edit`)}>
                  Edit
                </Button>
                <Button variant="success" onClick={handleSendInvoice}>
                  Send Invoice
                </Button>
                <Button variant="danger" onClick={handleDelete}>
                  Delete
                </Button>
              </>
            )}
            {(invoice.status === InvoiceStatus.SENT || invoice.status === InvoiceStatus.OVERDUE) && (
              <>
                {invoice.balanceDue > 0 && (
                  <Button variant="primary" onClick={() => setIsPaymentModalOpen(true)}>
                    Record Payment
                  </Button>
                )}
                <Button variant="secondary" onClick={handleMarkAsPaid}>
                  Mark as Paid
                </Button>
                <Button variant="danger" onClick={handleCancel}>
                  Cancel
                </Button>
              </>
            )}
          </div>
        }
      />

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white shadow rounded-lg overflow-hidden">
          {/* Invoice Header */}
          <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Issue Date</p>
                <p className="text-sm text-gray-900">{new Date(invoice.issueDate).toLocaleDateString()}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Due Date</p>
                <p className="text-sm text-gray-900">{new Date(invoice.dueDate).toLocaleDateString()}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Status</p>
                <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                  invoice.status === InvoiceStatus.PAID
                    ? 'bg-green-100 text-green-800'
                    : invoice.status === InvoiceStatus.OVERDUE
                    ? 'bg-red-100 text-red-800'
                    : invoice.status === InvoiceStatus.SENT
                    ? 'bg-blue-100 text-blue-800'
                    : invoice.status === 'CANCELLED'
                    ? 'bg-yellow-100 text-yellow-800'
                    : 'bg-gray-100 text-gray-800'
                }`}>
                  {invoice.status}
                </span>
              </div>
            </div>
          </div>

          {/* Line Items */}
          <div className="px-6 py-4">
            <h3 className="text-lg font-medium text-gray-900 mb-4">Line Items</h3>
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Quantity</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Unit Price</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Total</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {invoice.lineItems.map(item => (
                  <tr key={item.id}>
                    <td className="px-4 py-3 text-sm text-gray-900">{item.description}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 text-right">{item.quantity}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 text-right">${formatCurrency(item.unitPrice)}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 text-right font-medium">${formatCurrency(item.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Totals */}
          <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
            <div className="flex justify-end">
              <div className="w-64 space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Subtotal:</span>
                  <span className="text-sm font-medium">${formatCurrency(invoice.subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Tax:</span>
                  <span className="text-sm font-medium">${formatCurrency(invoice.taxAmount)}</span>
                </div>
                <div className="flex justify-between border-t pt-2">
                  <span className="text-base font-semibold">Total:</span>
                  <span className="text-base font-bold">${formatCurrency(invoice.totalAmount)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Amount Paid:</span>
                  <span className="text-sm font-medium text-green-600">${formatCurrency(invoice.totalAmount - invoice.balanceDue)}</span>
                </div>
                <div className="flex justify-between border-t pt-2">
                  <span className="text-base font-semibold">Balance Due:</span>
                  <span className="text-base font-bold text-red-600">${formatCurrency(invoice.balanceDue)}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Payments */}
          {payments.length > 0 && (
            <div className="px-6 py-4 border-t border-gray-200">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Payment History</h3>
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Method</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amount</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Reference</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {payments.map(payment => (
                    <tr key={payment.id}>
                      <td className="px-4 py-3 text-sm text-gray-900">
                        {new Date(payment.paymentDate).toLocaleDateString()}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-900">{payment.paymentMethod}</td>
                      <td className="px-4 py-3 text-sm text-gray-900 text-right font-medium">
                        ${formatCurrency(payment.amount)}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-900">{payment.referenceNumber || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Payment Modal */}
      <Modal
        isOpen={isPaymentModalOpen}
        onClose={() => setIsPaymentModalOpen(false)}
        title="Record Payment"
        size="md"
      >
        <form onSubmit={handlePaymentSubmit} className="space-y-4">
          <Input
            label="Amount"
            type="number"
            value={paymentData.amount}
            onChange={(e) => {
              const value = parseFloat(e.target.value);
              setPaymentData({ ...paymentData, amount: isNaN(value) ? 0 : value });
            }}
            min="0"
            max={invoice.balanceDue}
            step="0.01"
            required
            helperText={`Maximum: $${invoice.balanceDue.toFixed(2)}`}
          />

          <Input
            label="Payment Date"
            type="date"
            value={paymentData.paymentDate}
            onChange={(e) => setPaymentData({ ...paymentData, paymentDate: e.target.value })}
            max={getTodayDate()}
            required
          />

          <Select
            label="Payment Method"
            value={paymentData.paymentMethod}
            onChange={(e) => setPaymentData({ ...paymentData, paymentMethod: e.target.value as PaymentMethod })}
            options={Object.values(PaymentMethod).map(method => ({
              value: method,
              label: method.replace('_', ' ')
            }))}
            required
          />

          <Input
            label="Reference Number"
            value={paymentData.referenceNumber}
            onChange={(e) => setPaymentData({ ...paymentData, referenceNumber: e.target.value })}
            placeholder="Check number, transaction ID, etc."
          />

          <div className="flex justify-end space-x-3 pt-4">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsPaymentModalOpen(false)}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" disabled={paymentLoading}>
              {paymentLoading ? 'Recording...' : 'Record Payment'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
