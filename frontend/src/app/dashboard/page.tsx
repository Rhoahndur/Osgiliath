'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Header } from '@/components/layout/Header';
import { Button } from '@/components/shared/Button';
import { customerService } from '@/services/customerService';
import { invoiceService } from '@/services/invoiceService';
import { Customer } from '@/models/Customer';
import { Invoice, InvoiceStatus } from '@/models/Invoice';

export default function DashboardPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalCustomers: 0,
    totalInvoices: 0,
    draftInvoices: 0,
    paidInvoices: 0,
    overdueInvoices: 0
  });
  const [recentInvoices, setRecentInvoices] = useState<Invoice[]>([]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      // Spring Data uses 0-based page numbers
      const [customersResponse, invoicesResponse] = await Promise.all([
        customerService.getCustomers(0, 1),
        invoiceService.getInvoices(0, 5)
      ]);

      const allInvoicesResponse = await invoiceService.getInvoices(0, 100);

      // Handle different response formats:
      // 1. Array response: [invoice1, invoice2, ...]
      // 2. Spring Page response: { content: [...], totalElements: number }
      const invoices = Array.isArray(allInvoicesResponse)
        ? allInvoicesResponse
        : (allInvoicesResponse.content || []);

      const recentInvoicesData = Array.isArray(invoicesResponse)
        ? invoicesResponse
        : (invoicesResponse.content || []);

      setStats({
        totalCustomers: customersResponse.totalElements || 0,
        totalInvoices: Array.isArray(allInvoicesResponse)
          ? allInvoicesResponse.length
          : (allInvoicesResponse.totalElements || 0),
        draftInvoices: invoices.filter(i => i.status === InvoiceStatus.DRAFT).length,
        paidInvoices: invoices.filter(i => i.status === InvoiceStatus.PAID).length,
        overdueInvoices: invoices.filter(i => i.status === InvoiceStatus.OVERDUE).length
      });

      setRecentInvoices(recentInvoicesData);
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
      // Set empty arrays on error
      setRecentInvoices([]);
    } finally {
      setLoading(false);
    }
  };

  const StatCard = ({ title, value, color }: { title: string; value: number; color: string }) => (
    <div className="bg-white overflow-hidden shadow rounded-lg">
      <div className="p-5">
        <div className="flex items-center">
          <div className="flex-1">
            <dt className="text-sm font-medium text-gray-500 truncate">{title}</dt>
            <dd className={`mt-1 text-3xl font-semibold ${color}`}>{value}</dd>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div>
      <Header
        title="Dashboard"
        subtitle="Welcome to Osgiliath"
        action={
          <div className="space-x-2">
            <Link href="/customers/new">
              <Button variant="secondary">New Customer</Button>
            </Link>
            <Link href="/invoices/new">
              <Button variant="primary">New Invoice</Button>
            </Link>
          </div>
        }
      />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {loading ? (
          <div className="text-center py-12">
            <p className="text-gray-500">Loading dashboard...</p>
          </div>
        ) : (
          <>
            {/* Stats Grid */}
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-5 mb-8">
              <StatCard title="Total Customers" value={stats.totalCustomers} color="text-blue-600" />
              <StatCard title="Total Invoices" value={stats.totalInvoices} color="text-gray-900" />
              <StatCard title="Draft" value={stats.draftInvoices} color="text-gray-600" />
              <StatCard title="Paid" value={stats.paidInvoices} color="text-green-600" />
              <StatCard title="Overdue" value={stats.overdueInvoices} color="text-red-600" />
            </div>

            {/* Recent Invoices */}
            <div className="bg-white shadow rounded-lg">
              <div className="px-4 py-5 border-b border-gray-200 sm:px-6">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg leading-6 font-medium text-gray-900">
                    Recent Invoices
                  </h3>
                  <Link href="/invoices">
                    <Button variant="secondary" size="sm">View All</Button>
                  </Link>
                </div>
              </div>
              <div className="overflow-hidden">
                {recentInvoices.length === 0 ? (
                  <div className="text-center py-12">
                    <p className="text-gray-500">No invoices yet</p>
                    <Link href="/invoices/new">
                      <Button variant="primary" className="mt-4">
                        Create Your First Invoice
                      </Button>
                    </Link>
                  </div>
                ) : (
                  <ul className="divide-y divide-gray-200">
                    {recentInvoices.map(invoice => (
                      <li
                        key={invoice.id}
                        onClick={() => router.push(`/invoices/${invoice.id}`)}
                        className="px-6 py-4 hover:bg-gray-50 cursor-pointer"
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex-1">
                            <p className="text-sm font-medium text-blue-600">
                              {invoice.invoiceNumber}
                            </p>
                            <p className="text-sm text-gray-500">{invoice.customerName}</p>
                          </div>
                          <div className="flex items-center space-x-4">
                            <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                              invoice.status === InvoiceStatus.PAID
                                ? 'bg-green-100 text-green-800'
                                : invoice.status === InvoiceStatus.OVERDUE
                                ? 'bg-red-100 text-red-800'
                                : 'bg-gray-100 text-gray-800'
                            }`}>
                              {invoice.status}
                            </span>
                            <p className="text-sm font-semibold text-gray-900">
                              ${invoice.totalAmount.toFixed(2)}
                            </p>
                          </div>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
