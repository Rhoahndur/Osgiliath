'use client';

import { useState, useEffect } from 'react';
import { customerService } from '@/services/customerService';
import { Customer } from '@/models/Customer';

export const useCustomerListViewModel = () => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadCustomers();
  }, [page, searchTerm]);

  const loadCustomers = async () => {
    try {
      setLoading(true);
      setError(null);
      // Spring Data uses 0-based page numbers, but UI uses 1-based
      const response = await customerService.getCustomers(page - 1, pageSize, searchTerm || undefined);
      // Handle Spring Page response structure (content, totalElements)
      setCustomers(response.content || []);
      setTotal(response.totalElements || 0);
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to load customers';
      setError(errorMessage);
      setCustomers([]); // Set empty array on error
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (term: string) => {
    setSearchTerm(term);
    setPage(1); // Reset to first page when searching
  };

  const deleteCustomer = async (id: string) => {
    try {
      setError(null);
      await customerService.deleteCustomer(id);
      await loadCustomers();
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to delete customer';
      setError(errorMessage);
      throw new Error(errorMessage);
    }
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

  return {
    customers,
    loading,
    error,
    page,
    pageSize,
    total,
    totalPages: Math.ceil(total / pageSize) || 1, // Ensure at least 1 page
    searchTerm,
    deleteCustomer,
    refreshCustomers: loadCustomers,
    nextPage,
    prevPage,
    handleSearch
  };
};
