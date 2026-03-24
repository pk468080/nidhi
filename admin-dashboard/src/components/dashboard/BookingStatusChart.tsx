'use client';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Card, CardHeader, CardTitle } from '@/components/ui/Card';

const data = [
  { name: 'Completed', value: 420, color: '#10b981' },
  { name: 'Pending', value: 80, color: '#f59e0b' },
  { name: 'In Progress', value: 60, color: '#8b5cf6' },
  { name: 'Cancelled', value: 40, color: '#ef4444' },
  { name: 'Disputed', value: 15, color: '#f97316' },
];

export function BookingStatusChart() {
  return (
    <Card>
      <CardHeader><CardTitle>Booking Status Distribution</CardTitle></CardHeader>
      <ResponsiveContainer width="100%" height={280}>
        <PieChart>
          <Pie data={data} cx="50%" cy="50%" innerRadius={70} outerRadius={110} paddingAngle={3} dataKey="value">
            {data.map((entry, i) => <Cell key={i} fill={entry.color} />)}
          </Pie>
          <Tooltip formatter={(v: number) => [v, 'Bookings']} />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </Card>
  );
}
