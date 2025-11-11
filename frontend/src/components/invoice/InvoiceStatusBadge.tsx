import React from 'react';

type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED';

interface Props {
  status: InvoiceStatus;
}

export const InvoiceStatusBadge: React.FC<Props> = ({ status }) => {
  const colorMap = {
    DRAFT: 'bg-gray-100 text-gray-700',
    SENT: 'bg-blue-100 text-blue-700',
    PAID: 'bg-green-100 text-green-700',
    OVERDUE: 'bg-red-100 text-red-700',
    CANCELLED: 'bg-gray-500 text-white'
  };

  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${colorMap[status]}`}>
      {status}
    </span>
  );
};
