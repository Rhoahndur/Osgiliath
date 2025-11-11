export interface MonthlyRevenueDto {
  month: string; // Format: "2024-01"
  revenue: number;
}

export interface TopCustomerDto {
  customerId: string;
  customerName: string;
  totalRevenue: number;
  invoiceCount: number;
}

export type InvoiceStatusBreakdown = {
  [key: string]: number;
};
