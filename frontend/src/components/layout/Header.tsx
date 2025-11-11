'use client';

import React from 'react';
import Image from 'next/image';

interface HeaderProps {
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
}

export const Header: React.FC<HeaderProps> = ({ title, subtitle, action }) => {
  return (
    <div className="bg-white shadow">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="md:flex md:items-center md:justify-between">
          <div className="flex-1 min-w-0 flex items-center gap-4">
            <Image
              src="/shield-icon.svg"
              alt="Osgiliath Shield"
              width={32}
              height={38}
              className="flex-shrink-0"
            />
            <div>
              <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
                {title}
              </h1>
              {subtitle && (
                <p className="mt-1 text-sm text-gray-500">{subtitle}</p>
              )}
            </div>
          </div>
          {action && (
            <div className="mt-4 flex md:mt-0 md:ml-4">
              {action}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
