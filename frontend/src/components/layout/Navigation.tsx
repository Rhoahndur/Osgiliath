'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import clsx from 'clsx';

interface NavLinkProps {
  href: string;
  children: React.ReactNode;
}

const NavLink: React.FC<NavLinkProps> = ({ href, children }) => {
  const pathname = usePathname();
  const isActive = pathname === href || pathname?.startsWith(href + '/');

  return (
    <Link
      href={href}
      className={clsx(
        'px-3 py-2 rounded-md text-sm font-medium transition-colors',
        isActive
          ? 'bg-blue-700 text-white'
          : 'text-gray-300 hover:bg-blue-600 hover:text-white'
      )}
    >
      {children}
    </Link>
  );
};

interface NavigationProps {
  onLogout: () => void;
  userName?: string;
}

export const Navigation: React.FC<NavigationProps> = ({ onLogout, userName }) => {
  const displayName = userName === 'admin' ? 'Administrator' : userName;

  return (
    <nav className="bg-blue-800 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center">
            <Link href="/dashboard" className="flex-shrink-0">
              <span className="text-white text-xl font-bold">Osgiliath</span>
            </Link>
            <div className="ml-10 flex items-baseline space-x-4">
              <NavLink href="/dashboard">Dashboard</NavLink>
              <NavLink href="/customers">Customers</NavLink>
              <NavLink href="/invoices">Invoices</NavLink>
              <NavLink href="/reports">Reports</NavLink>
            </div>
          </div>
          <div className="flex items-center space-x-4">
            {displayName && (
              <span className="text-gray-300 text-sm">
                Welcome, {displayName}
              </span>
            )}
            <button
              onClick={onLogout}
              className="px-3 py-2 rounded-md text-sm font-medium text-gray-300 hover:bg-blue-600 hover:text-white transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
};
