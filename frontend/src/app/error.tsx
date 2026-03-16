'use client';

import { useEffect } from 'react';
import { Button } from '@/components/shared/Button';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center px-6">
        <h1 className="text-6xl font-bold text-gray-300 mb-4">500</h1>
        <h2 className="text-2xl font-semibold text-gray-800 mb-2">Something went wrong</h2>
        <p className="text-gray-500 mb-8 max-w-md">
          An unexpected error occurred. Please try again or return to the dashboard.
        </p>
        <div className="flex justify-center space-x-4">
          <Button variant="primary" onClick={reset}>
            Try Again
          </Button>
          <Button variant="secondary" onClick={() => (window.location.href = '/dashboard')}>
            Go to Dashboard
          </Button>
        </div>
      </div>
    </div>
  );
}
