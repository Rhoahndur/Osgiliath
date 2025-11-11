'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { invoiceService } from '@/services/invoiceService';
import { Invoice, InvoiceStatus } from '@/models/Invoice';

export const useInvoiceListViewModel = () => {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [customerIdFilter, setCustomerIdFilter] = useState<string | undefined>(undefined);
  const [fromDateFilter, setFromDateFilter] = useState<string | undefined>(undefined);
  const [toDateFilter, setToDateFilter] = useState<string | undefined>(undefined);
  const [sortBy, setSortBy] = useState<string>('issueDate');
  const [sortDirection, setSortDirection] = useState<string>('DESC');

  // Initialize filters from URL on mount
  useEffect(() => {
    const status = searchParams.get('status') || undefined;
    const customerId = searchParams.get('customerId') || undefined;
    const fromDate = searchParams.get('fromDate') || undefined;
    const toDate = searchParams.get('toDate') || undefined;
    const sort = searchParams.get('sortBy') || 'issueDate';
    const direction = searchParams.get('sortDirection') || 'DESC';
    const pageParam = searchParams.get('page');

    setStatusFilter(status);
    setCustomerIdFilter(customerId);
    setFromDateFilter(fromDate);
    setToDateFilter(toDate);
    setSortBy(sort);
    setSortDirection(direction);
    if (pageParam) setPage(parseInt(pageParam, 10));
  }, []);

  useEffect(() => {
    loadInvoices();
    updateURL();
  }, [page, statusFilter, customerIdFilter, fromDateFilter, toDateFilter, sortBy, sortDirection]);

  const updateURL = () => {
    const params = new URLSearchParams();
    if (statusFilter) params.set('status', statusFilter);
    if (customerIdFilter) params.set('customerId', customerIdFilter);
    if (fromDateFilter) params.set('fromDate', fromDateFilter);
    if (toDateFilter) params.set('toDate', toDateFilter);
    if (sortBy !== 'issueDate') params.set('sortBy', sortBy);
    if (sortDirection !== 'DESC') params.set('sortDirection', sortDirection);
    if (page !== 1) params.set('page', page.toString());

    const queryString = params.toString();
    const newUrl = queryString ? `/invoices?${queryString}` : '/invoices';
    router.push(newUrl, { scroll: false });
  };

  const loadInvoices = async () => {
    try {
      setLoading(true);
      setError(null);
      // Spring Data uses 0-based page numbers, but UI uses 1-based
      const response = await invoiceService.getInvoices(
        page - 1,
        pageSize,
        statusFilter,
        customerIdFilter,
        fromDateFilter,
        toDateFilter,
        sortBy,
        sortDirection
      );
      // Handle different response formats:
      // 1. Array response: [invoice1, invoice2, ...]
      // 2. Spring Page response: { content: [...], totalElements: number }
      if (Array.isArray(response)) {
        setInvoices(response);
        setTotal(response.length);
      } else {
        setInvoices(response.content || []);
        setTotal(response.totalElements || 0);
      }
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to load invoices';
      setError(errorMessage);
      setInvoices([]); // Set empty array on error
    } finally {
      setLoading(false);
    }
  };

  const sendInvoice = async (id: string) => {
    try {
      setError(null);
      await invoiceService.sendInvoice(id);
      await loadInvoices();
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to send invoice';
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  };

  const filterByStatus = (status: string | undefined) => {
    setStatusFilter(status);
    setPage(1);
  };

  const filterByCustomer = (customerId: string | undefined) => {
    setCustomerIdFilter(customerId);
    setPage(1);
  };

  const filterByDateRange = (fromDate: string | undefined, toDate: string | undefined) => {
    setFromDateFilter(fromDate);
    setToDateFilter(toDate);
    setPage(1);
  };

  const clearFilters = () => {
    setStatusFilter(undefined);
    setCustomerIdFilter(undefined);
    setFromDateFilter(undefined);
    setToDateFilter(undefined);
    setPage(1);
  };

  const handleSort = (field: string) => {
    if (sortBy === field) {
      // Toggle direction
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC');
    } else {
      // New field, default to DESC
      setSortBy(field);
      setSortDirection('DESC');
    }
    setPage(1);
  };

  const nextPage = () => {
    if (page * pageSize < total) {
      setPage(page + 1);
    }
  };

  const prevPage = () => {
    if (page > 1) {
      setPage(page - 1);
    }
  };

  const getActiveFilterCount = () => {
    let count = 0;
    if (statusFilter) count++;
    if (customerIdFilter) count++;
    if (fromDateFilter) count++;
    if (toDateFilter) count++;
    return count;
  };

  return {
    invoices,
    loading,
    error,
    page,
    pageSize,
    total,
    totalPages: Math.ceil(total / pageSize) || 1, // Ensure at least 1 page
    statusFilter,
    customerIdFilter,
    fromDateFilter,
    toDateFilter,
    sortBy,
    sortDirection,
    activeFilterCount: getActiveFilterCount(),
    sendInvoice,
    filterByStatus,
    filterByCustomer,
    filterByDateRange,
    clearFilters,
    handleSort,
    refreshInvoices: loadInvoices,
    nextPage,
    prevPage
  };
};
