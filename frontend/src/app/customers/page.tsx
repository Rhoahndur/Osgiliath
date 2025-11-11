'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Header } from '@/components/layout/Header';
import { Button } from '@/components/shared/Button';
import { Input } from '@/components/shared/Input';
import { Table, Pagination } from '@/components/shared/Table';
import { useCustomerListViewModel } from '@/viewmodels/useCustomerListViewModel';
import { Customer } from '@/models/Customer';

export default function CustomersPage() {
  const router = useRouter();
  const {
    customers,
    loading,
    error,
    page,
    totalPages,
    searchTerm,
    deleteCustomer,
    nextPage,
    prevPage,
    handleSearch
  } = useCustomerListViewModel();

  const [localSearchTerm, setLocalSearchTerm] = useState(searchTerm);

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setLocalSearchTerm(value);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSearch(localSearchTerm);
  };

  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to delete this customer?')) {
      try {
        await deleteCustomer(id);
      } catch (err) {
        alert('Failed to delete customer');
      }
    }
  };

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'name', label: 'Name' },
    { key: 'email', label: 'Email' },
    { key: 'phone', label: 'Phone' },
    {
      key: 'actions',
      label: 'Actions',
      render: (_: any, customer: Customer) => (
        <div className="flex space-x-2">
          <Button
            size="sm"
            variant="secondary"
            onClick={(e) => {
              e.stopPropagation();
              router.push(`/customers/${customer.id}`);
            }}
          >
            View
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={(e) => {
              e.stopPropagation();
              handleDelete(customer.id);
            }}
          >
            Delete
          </Button>
        </div>
      )
    }
  ];

  return (
    <div>
      <Header
        title="Customers"
        subtitle="Manage your customer database"
        action={
          <Link href="/customers/new">
            <Button variant="primary">Add Customer</Button>
          </Link>
        }
      />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Search Bar */}
        <div className="mb-6 bg-white shadow rounded-lg p-4">
          <form onSubmit={handleSearchSubmit} className="flex gap-4">
            <div className="flex-1">
              <Input
                label="Search"
                type="text"
                value={localSearchTerm}
                onChange={handleSearchChange}
                placeholder="Search by name or email..."
              />
            </div>
            <div className="flex items-end">
              <Button type="submit" variant="primary">
                Search
              </Button>
            </div>
            {searchTerm && (
              <div className="flex items-end">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setLocalSearchTerm('');
                    handleSearch('');
                  }}
                >
                  Clear
                </Button>
              </div>
            )}
          </form>
        </div>

        {error && (
          <div className="mb-4 rounded-md bg-red-50 p-4">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        {loading ? (
          <div className="text-center py-12">
            <p className="text-gray-500">Loading customers...</p>
          </div>
        ) : (
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <Table
              columns={columns}
              data={customers}
              keyField="id"
              onRowClick={(customer) => router.push(`/customers/${customer.id}`)}
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
