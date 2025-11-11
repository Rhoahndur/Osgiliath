import apiClient from './apiClient';
import {
  Invoice,
  CreateInvoiceRequest,
  UpdateInvoiceRequest,
  InvoiceListResponse,
  CreateLineItemRequest,
  LineItem
} from '@/models/Invoice';

export const invoiceService = {
  async getInvoices(
    page: number = 1,
    pageSize: number = 10,
    status?: string,
    customerId?: string,
    fromDate?: string,
    toDate?: string,
    sortBy?: string,
    sortDirection?: string
  ): Promise<InvoiceListResponse> {
    const response = await apiClient.get<InvoiceListResponse>('/invoices', {
      params: {
        page,
        size: pageSize,
        ...(status && { status }),
        ...(customerId && { customerId }),
        ...(fromDate && { fromDate }),
        ...(toDate && { toDate }),
        ...(sortBy && { sortBy }),
        ...(sortDirection && { sortDirection })
      }
    });
    return response.data;
  },

  async getInvoice(id: string): Promise<Invoice> {
    const response = await apiClient.get<Invoice>(`/invoices/${id}`);
    return response.data;
  },

  async createInvoice(invoice: CreateInvoiceRequest): Promise<Invoice> {
    const response = await apiClient.post<Invoice>('/invoices', invoice);
    return response.data;
  },

  async updateInvoice(id: string, invoice: UpdateInvoiceRequest): Promise<Invoice> {
    const response = await apiClient.put<Invoice>(`/invoices/${id}`, invoice);
    return response.data;
  },

  async sendInvoice(id: string): Promise<Invoice> {
    const response = await apiClient.post<Invoice>(`/invoices/${id}/send`);
    return response.data;
  },

  async markInvoiceAsPaid(id: string): Promise<Invoice> {
    const response = await apiClient.post<Invoice>(`/invoices/${id}/mark-paid`);
    return response.data;
  },

  async cancelInvoice(id: string): Promise<Invoice> {
    const response = await apiClient.post<Invoice>(`/invoices/${id}/cancel`);
    return response.data;
  },

  async deleteInvoice(id: string): Promise<void> {
    await apiClient.delete(`/invoices/${id}`);
  },

  async addLineItem(invoiceId: string, lineItem: CreateLineItemRequest): Promise<LineItem> {
    const response = await apiClient.post<LineItem>(
      `/invoices/${invoiceId}/line-items`,
      lineItem
    );
    return response.data;
  },

  async deleteLineItem(invoiceId: string, lineItemId: number): Promise<void> {
    await apiClient.delete(`/invoices/${invoiceId}/line-items/${lineItemId}`);
  },

  async exportInvoiceToPdf(id: string, invoiceNumber: string): Promise<void> {
    const response = await apiClient.get(`/invoices/${id}/pdf`, {
      responseType: 'blob'
    });

    // Create a blob from the PDF data
    const blob = new Blob([response.data], { type: 'application/pdf' });

    // Check if File System Access API is supported (Chromium-based browsers)
    if ('showSaveFilePicker' in window) {
      try {
        // Show native file save dialog
        const handle = await (window as any).showSaveFilePicker({
          suggestedName: `invoice_${invoiceNumber}.pdf`,
          types: [
            {
              description: 'PDF Document',
              accept: { 'application/pdf': ['.pdf'] }
            }
          ]
        });

        // Write the file
        const writable = await handle.createWritable();
        await writable.write(blob);
        await writable.close();
        return;
      } catch (err: any) {
        // User cancelled the dialog or error occurred
        if (err.name === 'AbortError') {
          return; // User cancelled, don't show error
        }
        console.error('Error saving file:', err);
        // Fall through to traditional download method
      }
    }

    // Fallback: Traditional download method (for browsers without File System Access API)
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `invoice_${invoiceNumber}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
};
