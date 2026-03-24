'use client';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { Card, CardHeader, CardTitle } from '@/components/ui/Card';

const data = Array.from({ length: 30 }, (_, i) => ({
  date: `Day ${i + 1}`,
  revenue: Math.floor(15000 + Math.random() * 25000),
  bookings: Math.floor(20 + Math.random() * 50),
}));

export function RevenueChart() {
  return (
    <Card>
      <CardHeader><CardTitle>Revenue (Last 30 Days)</CardTitle></CardHeader>
      <ResponsiveContainer width="100%" height={280}>
        <LineChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis dataKey="date" tick={{ fontSize: 11 }} interval={4} />
          <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => `₹${(v/1000).toFixed(0)}k`} />
          <Tooltip formatter={(v: number, name: string) => [name === 'revenue' ? `₹${v.toLocaleString('en-IN')}` : v, name === 'revenue' ? 'Revenue' : 'Bookings']} />
          <Legend />
          <Line type="monotone" dataKey="revenue" stroke="#3b82f6" strokeWidth={2} dot={false} />
          <Line type="monotone" dataKey="bookings" stroke="#10b981" strokeWidth={2} dot={false} />
        </LineChart>
      </ResponsiveContainer>
    </Card>
  );
}
