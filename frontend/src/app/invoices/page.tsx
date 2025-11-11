'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Header } from '@/components/layout/Header';
import { Button } from '@/components/shared/Button';
import { Table, Pagination } from '@/components/shared/Table';
import { Select, Input } from '@/components/shared/Input';
import { useInvoiceListViewModel } from '@/viewmodels/useInvoiceListViewModel';
import { Invoice, InvoiceStatus } from '@/models/Invoice';
import { formatCurrency } from '@/utils/format';
import { customerService } from '@/services/customerService';
import { Customer } from '@/models/Customer';
import { InvoiceStatusBadge } from '@/components/invoice/InvoiceStatusBadge';

export default function InvoicesPage() {
  const router = useRouter();
  const {
    invoices,
    loading,
    error,
    page,
    totalPages,
    statusFilter,
    customerIdFilter,
    fromDateFilter,
    toDateFilter,
    sortBy,
    sortDirection,
    activeFilterCount,
    sendInvoice,
    filterByStatus,
    filterByCustomer,
    filterByDateRange,
    clearFilters,
    handleSort,
    nextPage,
    prevPage
  } = useInvoiceListViewModel();

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [filtersExpanded, setFiltersExpanded] = useState(false);
  const [localFromDate, setLocalFromDate] = useState(fromDateFilter || '');
  const [localToDate, setLocalToDate] = useState(toDateFilter || '');

  // Load customers for dropdown
  useEffect(() => {
    const loadCustomers = async () => {
      try {
        const response = await customerService.getCustomers(0, 1000);
        setCustomers(response.content || []);
      } catch (err) {
        console.error('Failed to load customers', err);
      }
    };
    loadCustomers();
  }, []);

  // Sync local date state with filter state
  useEffect(() => {
    setLocalFromDate(fromDateFilter || '');
    setLocalToDate(toDateFilter || '');
  }, [fromDateFilter, toDateFilter]);

  // Auto-expand filters if any are active
  useEffect(() => {
    if (activeFilterCount > 0) {
      setFiltersExpanded(true);
    }
  }, []);

  const handleSendInvoice = async (id: string) => {
    if (confirm('Are you sure you want to send this invoice?')) {
      try {
        await sendInvoice(id);
      } catch (err) {
        alert('Failed to send invoice');
      }
    }
  };

  const handleApplyDateFilter = () => {
    filterByDateRange(localFromDate || undefined, localToDate || undefined);
  };

  const handleClearFilters = () => {
    clearFilters();
    setLocalFromDate('');
    setLocalToDate('');
  };

  const columns = [
    { key: 'invoiceNumber', label: 'Invoice #' },
    { key: 'customerName', label: 'Customer' },
    {
      key: 'issueDate',
      label: 'Issue Date',
      render: (value: string) => new Date(value).toLocaleDateString()
    },
    {
      key: 'dueDate',
      label: 'Due Date',
      render: (value: string) => new Date(value).toLocaleDateString()
    },
    {
      key: 'status',
      label: 'Status',
      render: (value: InvoiceStatus) => <InvoiceStatusBadge status={value} />
    },
    {
      key: 'totalAmount',
      label: 'Total',
      render: (value: number) => `$${formatCurrency(value)}`
    },
    {
      key: 'balanceDue',
      label: 'Balance',
      render: (value: number) => `$${formatCurrency(value)}`
    },
    {
      key: 'actions',
      label: 'Actions',
      render: (_: any, invoice: Invoice) => (
        <div className="flex space-x-2">
          <Button
            size="sm"
            variant="secondary"
            onClick={(e) => {
              e.stopPropagation();
              router.push(`/invoices/${invoice.id}`);
            }}
          >
            View
          </Button>
          {invoice.status === InvoiceStatus.DRAFT && (
            <Button
              size="sm"
              variant="success"
              onClick={(e) => {
                e.stopPropagation();
                handleSendInvoice(invoice.id);
              }}
              disabled={!invoice.lineItems || invoice.lineItems.length === 0}
            >
              Send
            </Button>
          )}
        </div>
      )
    }
  ];

  return (
    <div>
      <Header
        title="Invoices"
        subtitle="Manage your invoices"
        action={
          <Link href="/invoices/new">
            <Button variant="primary">Create Invoice</Button>
          </Link>
        }
      />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Filters */}
        <div className="mb-6 bg-white shadow rounded-lg overflow-hidden">
          {/* Filter Header */}
          <div className="px-4 py-3 border-b border-gray-200 bg-gray-50">
            <div className="flex items-center justify-between">
              <button
                onClick={() => setFiltersExpanded(!filtersExpanded)}
                className="flex items-center space-x-2 text-sm font-medium text-gray-700 hover:text-gray-900"
              >
                <svg
                  className={`h-5 w-5 transform transition-transform ${filtersExpanded ? 'rotate-90' : ''}`}
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
                <span>Filters</span>
                {activeFilterCount > 0 && (
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                    {activeFilterCount} active
                  </span>
                )}
              </button>
              {activeFilterCount > 0 && (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={handleClearFilters}
                >
                  Clear All Filters
                </Button>
              )}
            </div>
          </div>

          {/* Filter Content */}
          {filtersExpanded && (
            <div className="p-4">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
                <Select
                  label="Status"
                  value={statusFilter || ''}
                  onChange={(e) => filterByStatus(e.target.value || undefined)}
                  options={[
                    { value: '', label: 'All Statuses' },
                    { value: InvoiceStatus.DRAFT, label: 'Draft' },
                    { value: InvoiceStatus.SENT, label: 'Sent' },
                    { value: InvoiceStatus.PAID, label: 'Paid' },
                    { value: InvoiceStatus.OVERDUE, label: 'Overdue' },
                    { value: 'CANCELLED', label: 'Cancelled' }
                  ]}
                />
                <Select
                  label="Customer"
                  value={customerIdFilter || ''}
                  onChange={(e) => filterByCustomer(e.target.value || undefined)}
                  options={[
                    { value: '', label: 'All Customers' },
                    ...customers.map(customer => ({
                      value: customer.id,
                      label: customer.name
                    }))
                  ]}
                />
                <Input
                  type="date"
                  label="From Date"
                  value={localFromDate}
                  onChange={(e) => setLocalFromDate(e.target.value)}
                  onBlur={handleApplyDateFilter}
                />
                <Input
                  type="date"
                  label="To Date"
                  value={localToDate}
                  onChange={(e) => setLocalToDate(e.target.value)}
                  onBlur={handleApplyDateFilter}
                />
              </div>

              {/* Sort Options */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t border-gray-200">
                <Select
                  label="Sort By"
                  value={sortBy}
                  onChange={(e) => handleSort(e.target.value)}
                  options={[
                    { value: 'issueDate', label: 'Issue Date' },
                    { value: 'dueDate', label: 'Due Date' },
                    { value: 'invoiceNumber', label: 'Invoice Number' },
                    { value: 'totalAmount', label: 'Total Amount' },
                    { value: 'status', label: 'Status' }
                  ]}
                />
                <Select
                  label="Sort Direction"
                  value={sortDirection}
                  onChange={(e) => {
                    const currentSort = sortBy;
                    handleSort(currentSort);
                  }}
                  options={[
                    { value: 'DESC', label: 'Descending' },
                    { value: 'ASC', label: 'Ascending' }
                  ]}
                />
              </div>
            </div>
          )}
        </div>

        {error && (
          <div className="mb-4 rounded-md bg-red-50 p-4">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        {loading ? (
          <div className="text-center py-12">
            <p className="text-gray-500">Loading invoices...</p>
          </div>
        ) : (
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <Table
              columns={columns}
              data={invoices}
              keyField="id"
              onRowClick={(invoice) => router.push(`/invoices/${invoice.id}`)}
            />
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onNextPage={nextPage}
              onPrevPage={prevPage}
            />
          </div>
        )}
      </div>
    </div>
  );
}
