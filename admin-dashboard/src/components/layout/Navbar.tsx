'use client';

import { useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { Menu, Bell, Search, LogOut, User, ChevronDown } from 'lucide-react';
import { signOut } from '@/lib/auth';
import { getInitials } from '@/utils/helpers';
import { cn } from '@/utils/helpers';

interface NavbarProps {
  onMenuClick: () => void;
  adminName?: string;
  adminEmail?: string;
}

const routeTitles: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/users/customers': 'Customers',
  '/users/providers': 'Providers',
  '/bookings': 'Bookings',
  '/payments': 'Payments',
  '/services': 'Services',
  '/reviews': 'Reviews',
  '/disputes': 'Disputes',
  '/analytics': 'Analytics',
  '/notifications': 'Notifications',
  '/settings': 'Settings',
  '/admins': 'Admin Users',
};

export function Navbar({ onMenuClick, adminName = 'Admin User', adminEmail = 'admin@nidhi.com' }: NavbarProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [profileOpen, setProfileOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);

  const title = routeTitles[pathname] ?? 'Dashboard';

  const handleSignOut = async () => {
    await signOut();
    router.replace('/login');
  };

  return (
    <header className="sticky top-0 z-10 flex h-16 items-center gap-4 border-b border-gray-200 bg-white px-4 lg:px-6">
      {/* Mobile menu button */}
      <button
        onClick={onMenuClick}
        className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 lg:hidden"
      >
        <Menu className="h-5 w-5" />
      </button>

      {/* Page title */}
      <h1 className="text-lg font-semibold text-gray-900 hidden sm:block">{title}</h1>

      {/* Spacer */}
      <div className="flex-1" />

      {/* Search */}
      <div className="relative hidden md:block">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input
          type="text"
          placeholder="Search..."
          className={cn(
            'w-64 rounded-lg border border-gray-200 bg-gray-50 py-2 pl-9 pr-4 text-sm',
            'focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500'
          )}
        />
      </div>

      {/* Notifications */}
      <div className="relative">
        <button
          onClick={() => { setNotifOpen((v) => !v); setProfileOpen(false); }}
          className="relative rounded-lg p-2 text-gray-500 hover:bg-gray-100"
        >
          <Bell className="h-5 w-5" />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-red-500" />
        </button>
        {notifOpen && (
          <div className="absolute right-0 mt-2 w-80 rounded-xl border border-gray-200 bg-white shadow-lg">
            <div className="border-b border-gray-100 px-4 py-3">
              <p className="font-semibold text-gray-900">Notifications</p>
            </div>
            <div className="divide-y divide-gray-50">
              {[
                { text: 'New provider verification request', time: '5 min ago', dot: 'bg-blue-500' },
                { text: '3 new disputes opened today', time: '1 hr ago', dot: 'bg-red-500' },
                { text: 'Monthly revenue report ready', time: '3 hrs ago', dot: 'bg-green-500' },
              ].map((n, i) => (
                <div key={i} className="flex gap-3 px-4 py-3 hover:bg-gray-50">
                  <div className={`mt-1.5 h-2 w-2 flex-shrink-0 rounded-full ${n.dot}`} />
                  <div>
                    <p className="text-sm text-gray-700">{n.text}</p>
                    <p className="text-xs text-gray-400">{n.time}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Profile dropdown */}
      <div className="relative">
        <button
          onClick={() => { setProfileOpen((v) => !v); setNotifOpen(false); }}
          className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-100"
        >
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-sm font-semibold text-white">
            {getInitials(adminName)}
          </div>
          <div className="hidden text-left sm:block">
            <p className="text-sm font-medium text-gray-900">{adminName}</p>
            <p className="text-xs text-gray-500">{adminEmail}</p>
          </div>
          <ChevronDown className="h-4 w-4 text-gray-400" />
        </button>
        {profileOpen && (
          <div className="absolute right-0 mt-2 w-48 rounded-xl border border-gray-200 bg-white shadow-lg">
            <div className="p-2">
              <button className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-gray-700 hover:bg-gray-50">
                <User className="h-4 w-4" />
                Profile
              </button>
              <button
                onClick={handleSignOut}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-red-600 hover:bg-red-50"
              >
                <LogOut className="h-4 w-4" />
                Sign Out
              </button>
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
