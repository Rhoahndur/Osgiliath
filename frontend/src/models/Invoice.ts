export interface LineItem {
  id: number;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number; // Backend uses lineTotal, not total
}

export interface Invoice {
  id: string; // UUID from backend
  customerId: string; // UUID from backend
  customerName?: string; // Optional, may not be included in list responses
  invoiceNumber: string;
  issueDate: string;
  dueDate: string;
  status: InvoiceStatus;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  balanceDue: number; // Backend uses balanceDue, not balance
  lineItems: LineItem[];
  createdAt: string;
  updatedAt: string;
}

export enum InvoiceStatus {
  DRAFT = 'DRAFT',
  SENT = 'SENT',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE',
  CANCELLED = 'CANCELLED'
}

export interface CreateInvoiceRequest {
  customerId: string; // UUID from backend
  issueDate: string;
  dueDate: string;
  taxRate?: number;
  lineItems: CreateLineItemRequest[];
}

export interface CreateLineItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
}

export interface UpdateInvoiceRequest {
  customerId?: string; // UUID from backend
  issueDate?: string;
  dueDate?: string;
  status?: InvoiceStatus;
}

// Spring Page response structure
export interface InvoiceListResponse {
  content: Invoice[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  pageable: {
    pageNumber: number;
    pageSize: number;
    offset: number;
  };
}
