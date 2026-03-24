'use client';
import { TrendingUp, TrendingDown } from 'lucide-react';
import { cn } from '@/utils/helpers';

interface MetricCardProps {
  title: string;
  value: string | number;
  change?: number;
  icon: React.ReactNode;
  iconBg?: string;
  suffix?: string;
}

export function MetricCard({ title, value, change, icon, iconBg = 'bg-blue-50', suffix }: MetricCardProps) {
  const isPositive = (change ?? 0) >= 0;
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <div className="flex items-center justify-between">
        <div className={cn('rounded-lg p-2.5', iconBg)}>{icon}</div>
        {change !== undefined && (
          <span className={cn('flex items-center gap-1 text-xs font-medium', isPositive ? 'text-green-600' : 'text-red-600')}>
            {isPositive ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
            {Math.abs(change)}%
          </span>
        )}
      </div>
      <div className="mt-4">
        <p className="text-sm font-medium text-gray-500">{title}</p>
        <p className="mt-1 text-2xl font-bold text-gray-900">{value}{suffix}</p>
      </div>
    </div>
  );
}
