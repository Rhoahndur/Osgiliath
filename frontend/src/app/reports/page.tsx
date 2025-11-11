'use client';

import { useEffect, useState } from 'react';
import { analyticsService } from '@/services/analyticsService';
import {
  MonthlyRevenueDto,
  TopCustomerDto,
  InvoiceStatusBreakdown
} from '@/models/Analytics';
import {
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';

type DateRange = 6 | 12 | 24 | 999;

const STATUS_COLORS: { [key: string]: string } = {
  DRAFT: '#94a3b8',    // gray
  SENT: '#3b82f6',     // blue
  PAID: '#10b981',     // green
  OVERDUE: '#ef4444',  // red
  CANCELLED: '#6b7280' // dark gray
};

const STATUS_LABELS: { [key: string]: string } = {
  DRAFT: 'Draft',
  SENT: 'Sent',
  PAID: 'Paid',
  OVERDUE: 'Overdue',
  CANCELLED: 'Cancelled'
};

export default function ReportsPage() {
  const [dateRange, setDateRange] = useState<DateRange>(12);
  const [revenueData, setRevenueData] = useState<MonthlyRevenueDto[]>([]);
  const [statusData, setStatusData] = useState<InvoiceStatusBreakdown>({});
  const [topCustomers, setTopCustomers] = useState<TopCustomerDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadAnalyticsData = async () => {
    setLoading(true);
    setError(null);

    try {
      const [revenue, status, customers] = await Promise.all([
        analyticsService.getRevenueOverTime(dateRange),
        analyticsService.getStatusBreakdown(),
        analyticsService.getTopCustomers(10)
      ]);

      setRevenueData(revenue);
      setStatusData(status);
      setTopCustomers(customers);
    } catch (err) {
      console.error('Error loading analytics:', err);
      setError('Failed to load analytics data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAnalyticsData();
  }, [dateRange]);

  // Transform status data for pie chart
  const pieChartData = Object.entries(statusData)
    .filter(([_, count]) => count > 0)
    .map(([status, count]) => ({
      name: STATUS_LABELS[status] || status,
      value: count,
      status: status
    }));

  // Calculate total revenue
  const totalRevenue = revenueData.reduce((sum, item) => sum + item.revenue, 0);

  // Calculate total invoices
  const totalInvoices = Object.values(statusData).reduce((sum, count) => sum + count, 0);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 p-8">
        <div className="max-w-7xl mx-auto">
          <div className="animate-pulse">
            <div className="h-8 bg-gray-200 rounded w-48 mb-8"></div>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
              <div className="bg-white rounded-lg shadow p-6 h-96"></div>
              <div className="bg-white rounded-lg shadow p-6 h-96"></div>
            </div>
            <div className="bg-white rounded-lg shadow p-6 h-96"></div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 p-8">
        <div className="max-w-7xl mx-auto">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
            {error}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Analytics & Reports</h1>
          <p className="text-gray-600 mt-2">View key metrics and insights for your business</p>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-sm font-medium text-gray-500 uppercase tracking-wide">Total Revenue</h3>
            <p className="text-3xl font-bold text-green-600 mt-2">
              ${totalRevenue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </p>
            <p className="text-sm text-gray-500 mt-1">
              {dateRange === 999
                ? (revenueData.length > 0 ? 'All time' : 'Last 6 months')
                : `Last ${dateRange} months`}
            </p>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-sm font-medium text-gray-500 uppercase tracking-wide">Total Invoices</h3>
            <p className="text-3xl font-bold text-blue-600 mt-2">{totalInvoices}</p>
            <p className="text-sm text-gray-500 mt-1">All statuses</p>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-sm font-medium text-gray-500 uppercase tracking-wide">Paid Invoices</h3>
            <p className="text-3xl font-bold text-green-600 mt-2">{statusData.PAID || 0}</p>
            <p className="text-sm text-gray-500 mt-1">
              {totalInvoices > 0 ? `${((statusData.PAID || 0) / totalInvoices * 100).toFixed(1)}%` : '0%'} of total
            </p>
          </div>
        </div>

        {/* Charts Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          {/* Revenue Over Time Chart */}
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold text-gray-900">Revenue Over Time</h2>
              <div className="flex gap-2">
                <button
                  onClick={() => setDateRange(6)}
                  className={`px-3 py-1 rounded-md text-sm font-medium ${
                    dateRange === 6
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  6M
                </button>
                <button
                  onClick={() => setDateRange(12)}
                  className={`px-3 py-1 rounded-md text-sm font-medium ${
                    dateRange === 12
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  1Y
                </button>
                <button
                  onClick={() => setDateRange(24)}
                  className={`px-3 py-1 rounded-md text-sm font-medium ${
                    dateRange === 24
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  2Y
                </button>
                <button
                  onClick={() => setDateRange(999)}
                  className={`px-3 py-1 rounded-md text-sm font-medium ${
                    dateRange === 999
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  Max
                </button>
              </div>
            </div>

            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={revenueData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis
                  dataKey="month"
                  tick={{ fontSize: 12 }}
                  angle={-45}
                  textAnchor="end"
                  height={60}
                />
                <YAxis
                  tick={{ fontSize: 12 }}
                  tickFormatter={(value) => `$${value.toLocaleString()}`}
                />
                <Tooltip
                  formatter={(value: number) => [
                    `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
                    'Revenue'
                  ]}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="revenue"
                  stroke="#10b981"
                  strokeWidth={2}
                  dot={{ fill: '#10b981' }}
                  name="Revenue"
                />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* Invoice Status Breakdown */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-bold text-gray-900 mb-6">Invoice Status Breakdown</h2>

            {pieChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={pieChartData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                    outerRadius={100}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {pieChartData.map((entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={STATUS_COLORS[entry.status] || '#94a3b8'}
                      />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-64 text-gray-500">
                No invoice data available
              </div>
            )}
          </div>
        </div>

        {/* Top Customers Table */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-bold text-gray-900 mb-6">Top Customers by Revenue</h2>

          {topCustomers.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Rank
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Customer Name
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Total Revenue
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Invoice Count
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Average Invoice
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {topCustomers.map((customer, index) => (
                    <tr key={customer.customerId} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        #{index + 1}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {customer.customerName}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-green-600">
                        ${customer.totalRevenue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {customer.invoiceCount}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                        ${(customer.totalRevenue / customer.invoiceCount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="text-center py-12 text-gray-500">
              No customer data available
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
