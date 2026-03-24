'use client';

import { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Users,
  User,
  Briefcase,
  Calendar,
  CreditCard,
  Wrench,
  Star,
  AlertTriangle,
  BarChart2,
  Bell,
  Settings,
  Shield,
  ChevronDown,
  ChevronRight,
  X,
  Zap,
} from 'lucide-react';
import { cn } from '@/utils/helpers';

interface NavItem {
  href?: string;
  label: string;
  icon: React.ReactNode;
  children?: { href: string; label: string; icon: React.ReactNode }[];
  badge?: number;
}

const navItems: NavItem[] = [
  { href: '/dashboard', label: 'Dashboard', icon: <LayoutDashboard className="h-5 w-5" /> },
  {
    label: 'Users',
    icon: <Users className="h-5 w-5" />,
    children: [
      { href: '/users/customers', label: 'Customers', icon: <User className="h-4 w-4" /> },
      { href: '/users/providers', label: 'Providers', icon: <Briefcase className="h-4 w-4" /> },
    ],
  },
  { href: '/bookings', label: 'Bookings', icon: <Calendar className="h-5 w-5" /> },
  { href: '/payments', label: 'Payments', icon: <CreditCard className="h-5 w-5" /> },
  { href: '/services', label: 'Services', icon: <Wrench className="h-5 w-5" /> },
  { href: '/reviews', label: 'Reviews', icon: <Star className="h-5 w-5" /> },
  { href: '/disputes', label: 'Disputes', icon: <AlertTriangle className="h-5 w-5" />, badge: 3 },
  { href: '/analytics', label: 'Analytics', icon: <BarChart2 className="h-5 w-5" /> },
  { href: '/notifications', label: 'Notifications', icon: <Bell className="h-5 w-5" /> },
  { href: '/settings', label: 'Settings', icon: <Settings className="h-5 w-5" /> },
  { href: '/admins', label: 'Admins', icon: <Shield className="h-5 w-5" /> },
];

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const pathname = usePathname();
  const [expandedItems, setExpandedItems] = useState<string[]>(['Users']);

  const toggleExpand = (label: string) => {
    setExpandedItems((prev) =>
      prev.includes(label) ? prev.filter((i) => i !== label) : [...prev, label]
    );
  };

  const isActive = (href?: string) => {
    if (!href) return false;
    return pathname === href || pathname.startsWith(href + '/');
  };

  const isParentActive = (item: NavItem) => {
    if (item.href) return isActive(item.href);
    return item.children?.some((c) => isActive(c.href)) ?? false;
  };

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 z-20 bg-black/50 lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-30 flex w-64 flex-col bg-[#1e2a3a] transition-transform duration-300',
          'lg:translate-x-0 lg:static lg:z-auto',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Logo */}
        <div className="flex h-16 items-center justify-between px-6 border-b border-white/10">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-500">
              <Zap className="h-5 w-5 text-white" />
            </div>
            <div>
              <span className="text-base font-bold text-white">Nidhi</span>
              <span className="ml-1 text-xs text-slate-400">Admin</span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-slate-400 hover:text-white lg:hidden"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto py-4 px-3">
          <ul className="space-y-0.5">
            {navItems.map((item) => (
              <li key={item.label}>
                {item.href ? (
                  <Link
                    href={item.href}
                    onClick={() => { if (window.innerWidth < 1024) onClose(); }}
                    className={cn(
                      'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                      isActive(item.href)
                        ? 'bg-blue-600 text-white'
                        : 'text-slate-400 hover:bg-white/10 hover:text-white'
                    )}
                  >
                    {item.icon}
                    <span className="flex-1">{item.label}</span>
                    {item.badge !== undefined && item.badge > 0 && (
                      <span className="rounded-full bg-red-500 px-1.5 py-0.5 text-xs text-white">
                        {item.badge}
                      </span>
                    )}
                  </Link>
                ) : (
                  <>
                    <button
                      onClick={() => toggleExpand(item.label)}
                      className={cn(
                        'flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                        isParentActive(item)
                          ? 'text-white'
                          : 'text-slate-400 hover:bg-white/10 hover:text-white'
                      )}
                    >
                      {item.icon}
                      <span className="flex-1 text-left">{item.label}</span>
                      {expandedItems.includes(item.label) ? (
                        <ChevronDown className="h-4 w-4" />
                      ) : (
                        <ChevronRight className="h-4 w-4" />
                      )}
                    </button>
                    {expandedItems.includes(item.label) && item.children && (
                      <ul className="ml-4 mt-0.5 space-y-0.5 border-l border-white/10 pl-3">
                        {item.children.map((child) => (
                          <li key={child.href}>
                            <Link
                              href={child.href}
                              onClick={() => { if (window.innerWidth < 1024) onClose(); }}
                              className={cn(
                                'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors',
                                isActive(child.href)
                                  ? 'bg-blue-600 text-white font-medium'
                                  : 'text-slate-400 hover:bg-white/10 hover:text-white'
                              )}
                            >
                              {child.icon}
                              {child.label}
                            </Link>
                          </li>
                        ))}
                      </ul>
                    )}
                  </>
                )}
              </li>
            ))}
          </ul>
        </nav>

        {/* Bottom info */}
        <div className="border-t border-white/10 px-4 py-3">
          <p className="text-xs text-slate-500">Nidhi Services v1.0</p>
        </div>
      </aside>
    </>
  );
}
