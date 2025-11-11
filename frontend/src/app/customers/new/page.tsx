'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Header } from '@/components/layout/Header';
import { Button } from '@/components/shared/Button';
import { Input, TextArea } from '@/components/shared/Input';
import { useCustomerFormViewModel } from '@/viewmodels/useCustomerFormViewModel';
import { CreateCustomerRequest } from '@/models/Customer';

export default function NewCustomerPage() {
  const router = useRouter();
  const { createCustomer, loading, error, validationErrors } = useCustomerFormViewModel();
  const [formData, setFormData] = useState<CreateCustomerRequest>({
    name: '',
    email: '',
    phone: '',
    address: ''
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await createCustomer(formData);
      // Redirect to customers list page since detail page doesn't exist yet
      router.push('/customers');
    } catch (err) {
      console.error('Failed to create customer:', err);
    }
  };

  return (
    <div>
      <Header title="New Customer" subtitle="Add a new customer to your database" />

      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white shadow rounded-lg p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="rounded-md bg-red-50 p-4">
                <p className="text-sm text-red-800">{error}</p>
              </div>
            )}

            <Input
              label="Name *"
              name="name"
              value={formData.name}
              onChange={handleChange}
              error={validationErrors.name}
              required
            />

            <Input
              label="Email *"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              error={validationErrors.email}
              required
            />

            <Input
              label="Phone"
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
            />

            <TextArea
              label="Address"
              name="address"
              value={formData.address}
              onChange={handleChange}
              rows={3}
            />

            <div className="flex justify-end space-x-3">
              <Button
                type="button"
                variant="secondary"
                onClick={() => router.push('/customers')}
              >
                Cancel
              </Button>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? 'Creating...' : 'Create Customer'}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
