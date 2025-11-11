export interface Customer {
  id: string; // UUID from backend
  name: string;
  email: string;
  phone?: string;
  address?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomerRequest {
  name: string;
  email: string;
  phone?: string;
  address?: string;
}

export interface UpdateCustomerRequest {
  name?: string;
  email?: string;
  phone?: string;
  address?: string;
}

// Spring Page response structure
export interface CustomerListResponse {
  content: Customer[];
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
