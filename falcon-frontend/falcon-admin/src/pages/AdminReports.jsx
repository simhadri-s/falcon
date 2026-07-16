import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowUpRight,
  BarChart3,
  Boxes,
  CalendarRange,
  CircleDollarSign,
  Download,
  LoaderCircle,
  MapPin,
  PackageOpen,
  ReceiptText,
  RefreshCw,
  ShoppingBag,
  TrendingDown,
  TrendingUp,
  Users2,
  Warehouse,
} from 'lucide-react';
import api from '../services/api';
import { downloadManagementReport } from '../services/reports';

const periodOptions = [
  { value: '7d', label: 'Last 7 Days' },
  { value: '30d', label: 'Last 30 Days' },
  { value: '90d', label: 'Last 90 Days' },
  { value: 'all', label: 'All Time' }
];

const sectionTabs = [
  {
    key: 'sales',
    label: 'Sales',
    icon: CircleDollarSign,
    tone: 'emerald',
    description: 'Revenue movement, order value, and discount exposure.'
  },
  {
    key: 'orders',
    label: 'Orders',
    icon: ReceiptText,
    tone: 'blue',
    description: 'Fulfillment velocity, destination spread, and processing mix.'
  },
  {
    key: 'products',
    label: 'Products',
    icon: ShoppingBag,
    tone: 'amber',
    description: 'Catalog depth, top sellers, and category contribution.'
  },
  {
    key: 'customers',
    label: 'Customers',
    icon: Users2,
    tone: 'violet',
    description: 'Buyer activity, repeat behavior, and highest value accounts.'
  },
  {
    key: 'stock',
    label: 'Stock',
    icon: Warehouse,
    tone: 'rose',
    description: 'Low stock exposure, stockouts, and expiry risk monitoring.'
  }
];

const toneStyles = {
  emerald: {
    icon: 'text-emerald-600',
    iconBg: 'bg-emerald-50',
    soft: 'bg-emerald-50 text-emerald-700 border-emerald-100',
    line: 'from-emerald-500/16 via-emerald-500/3 to-transparent',
    bar: 'from-emerald-500 via-emerald-400 to-emerald-300',
    dot: 'bg-emerald-500'
  },
  blue: {
    icon: 'text-blue-600',
    iconBg: 'bg-blue-50',
    soft: 'bg-blue-50 text-blue-700 border-blue-100',
    line: 'from-blue-500/16 via-blue-500/3 to-transparent',
    bar: 'from-blue-500 via-blue-400 to-blue-300',
    dot: 'bg-blue-500'
  },
  amber: {
    icon: 'text-amber-600',
    iconBg: 'bg-amber-50',
    soft: 'bg-amber-50 text-amber-700 border-amber-100',
    line: 'from-amber-500/16 via-amber-500/3 to-transparent',
    bar: 'from-amber-500 via-amber-400 to-amber-300',
    dot: 'bg-amber-500'
  },
  violet: {
    icon: 'text-violet-600',
    iconBg: 'bg-violet-50',
    soft: 'bg-violet-50 text-violet-700 border-violet-100',
    line: 'from-violet-500/16 via-violet-500/3 to-transparent',
    bar: 'from-violet-500 via-violet-400 to-violet-300',
    dot: 'bg-violet-500'
  },
  rose: {
    icon: 'text-rose-600',
    iconBg: 'bg-rose-50',
    soft: 'bg-rose-50 text-rose-700 border-rose-100',
    line: 'from-rose-500/16 via-rose-500/3 to-transparent',
    bar: 'from-rose-500 via-rose-400 to-rose-300',
    dot: 'bg-rose-500'
  },
  slate: {
    icon: 'text-slate-600',
    iconBg: 'bg-slate-100',
    soft: 'bg-slate-100 text-slate-700 border-slate-200',
    line: 'from-slate-500/16 via-slate-500/3 to-transparent',
    bar: 'from-slate-500 via-slate-400 to-slate-300',
    dot: 'bg-slate-500'
  }
};

export default function AdminReports() {
  const [period, setPeriod] = useState('30d');
  const [activeSection, setActiveSection] = useState('sales');
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState(null);

  const loadReport = async (selectedPeriod) => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/reports', { params: { period: selectedPeriod } });
      setReport(response.data);
      setLastUpdated(new Date());
    } catch (err) {
      console.error(err);
      setError('Unable to load reports right now. Please verify the backend and try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReport(period);
  }, [period]);

  const handleRefresh = () => {
    loadReport(period);
  };

  const handleDownloadPdf = async () => {
    if (downloading) return;
    setDownloading(true);
    try {
      await downloadManagementReport(period);
    } catch (err) {
      alert(err.message);
    } finally {
      setDownloading(false);
    }
  };

  const activeTab = useMemo(
    () => sectionTabs.find((tab) => tab.key === activeSection) || sectionTabs[0],
    [activeSection]
  );

  const headlineMetrics = useMemo(() => getHeadlineMetrics(report), [report]);
  const sectionStats = useMemo(() => getSectionStats(report, activeSection), [report, activeSection]);
  const sectionNarrative = useMemo(
    () => getSectionNarrative(report, activeSection),
    [report, activeSection]
  );
  const ActiveSectionIcon = activeTab.icon;

  return (
    <div className="relative flex flex-col h-full pb-6 px-6 pt-6 overflow-y-auto">
      <div className="mx-auto w-full max-w-[1480px]">
        <div className="space-y-6">
          <section className="bg-white rounded border border-gray-100 shadow-sm flex flex-col">
            <div className="p-6">
              <div className="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(320px,0.7fr)]">
                <div className="space-y-5">
                  <div className="flex flex-wrap items-center gap-3">
                    <span className="inline-flex items-center rounded-full border border-slate-200 bg-white/80 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">
                      Executive Reporting
                    </span>
                    <span className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white/75 px-3 py-1 text-xs font-medium text-slate-600">
                      <span className={`h-2 w-2 rounded-full ${loading ? 'bg-amber-400' : 'bg-emerald-500'}`} />
                      {loading ? 'Refreshing data' : `Synced ${formatTimestamp(lastUpdated)}`}
                    </span>
                  </div>

                  <div>
                    <h1 className="text-3xl font-semibold tracking-tight text-slate-950 sm:text-[2.4rem]">
                      Management reports
                    </h1>
                    <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600 sm:text-[15px]">
                      A polished view of commercial performance, operational flow, customer activity, and stock health for{' '}
                      <span className="font-semibold text-slate-900">{report?.periodLabel || 'the selected period'}</span>.
                    </p>
                  </div>

                  <div className="flex flex-wrap gap-2 rounded-2xl border border-slate-200/80 bg-white/85 p-2 shadow-sm">
                    {sectionTabs.map((tab) => {
                      const isActive = activeSection === tab.key;
                      const Icon = tab.icon;
                      const tone = toneStyles[tab.tone];
                      return (
                        <button
                          key={tab.key}
                          onClick={() => setActiveSection(tab.key)}
                          className={`inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all ${
                            isActive
                              ? 'bg-[#1E3A5F] text-white shadow-[0_14px_30px_-20px_rgba(30,58,95,0.9)]'
                              : 'text-slate-600 hover:bg-slate-50'
                          }`}
                        >
                          <span
                            className={`rounded-lg p-1.5 ${
                              isActive ? 'bg-white/10 text-white' : `${tone.iconBg} ${tone.icon}`
                            }`}
                          >
                            <Icon className="h-4 w-4" />
                          </span>
                          {tab.label}
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="rounded border border-gray-100 bg-white p-5 shadow-sm">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">
                        Report Controls
                      </p>
                      <p className="mt-2 text-sm font-medium text-slate-600">
                        Adjust the analysis window, refresh the feed, or export the current management pack.
                      </p>
                    </div>
                    <div className={`rounded-2xl p-3 ${toneStyles[activeTab.tone].iconBg} ${toneStyles[activeTab.tone].icon}`}>
                      <ActiveSectionIcon className="h-5 w-5" />
                    </div>
                  </div>

                  <div className="mt-5 space-y-4">
                    <div>
                      <label className="mb-2 block text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500">
                        Reporting Period
                      </label>
                      <select
                        value={period}
                        onChange={(e) => setPeriod(e.target.value)}
                        className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
                      >
                        {periodOptions.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="grid gap-3 sm:grid-cols-2">
                      <button
                        onClick={handleRefresh}
                        className="inline-flex items-center justify-center gap-2 rounded border border-gray-200 bg-white px-4 py-2 text-[0.85rem] font-medium text-gray-700 transition hover:bg-gray-50 shadow-sm"
                      >
                        <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                        Refresh
                      </button>
                      <button
                        onClick={handleDownloadPdf}
                        disabled={downloading}
                        className="inline-flex items-center justify-center gap-2 rounded bg-blue-600 px-4 py-2 text-[0.85rem] font-medium text-white transition hover:bg-blue-700 shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        <Download className={`h-4 w-4 ${downloading ? 'animate-bounce' : ''}`} />
                        {downloading ? 'Generating...' : 'Download PDF'}
                      </button>
                    </div>

                    <div className="rounded border border-gray-100 bg-gray-50 px-4 py-3">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500">
                        Active Focus
                      </p>
                      <p className="mt-2 text-sm font-semibold text-slate-900">{activeTab.label}</p>
                      <p className="mt-1 text-sm leading-6 text-slate-600">{activeTab.description}</p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                {headlineMetrics.map((metric) => (
                  <HeadlineMetricCard key={metric.title} metric={metric} />
                ))}
              </div>
            </div>
          </section>

          <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_330px]">
            <div className="space-y-6">
              {loading ? (
                <LoadingState />
              ) : error ? (
                <ErrorState error={error} onRetry={handleRefresh} />
              ) : (
                <>
                  {activeSection === 'sales' && <SalesSection report={report} />}
                  {activeSection === 'orders' && <OrdersSection report={report} />}
                  {activeSection === 'products' && <ProductsSection report={report} />}
                  {activeSection === 'customers' && <CustomersSection report={report} />}
                  {activeSection === 'stock' && <StockSection report={report} />}
                </>
              )}
            </div>

            <aside className="space-y-6">
              <InsightPanel
                title={`${activeTab.label} snapshot`}
                tone={activeTab.tone}
                subtitle={activeTab.description}
                items={sectionStats}
              />
              <NarrativePanel
                title="Management Notes"
                tone="slate"
                notes={sectionNarrative}
                periodLabel={report?.periodLabel}
              />
            </aside>
          </div>
        </div>
      </div>
    </div>
  );
}

function SalesSection({ report }) {
  const sales = report?.salesReport;

  return (
    <SectionShell
      title="Sales Performance"
      description="A high-level look at revenue quality, discount drag, and transaction efficiency."
      icon={CircleDollarSign}
      tone="emerald"
      meta={[
        { label: 'Trend points', value: `${sales?.timeline?.length || 0} checkpoints` },
        { label: 'Period', value: report?.periodLabel || 'Selected window' }
      ]}
    >
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricTile
          title="Gross Revenue"
          value={formatCurrency(sales?.grossRevenue)}
          subtext="Subtotal before discounts"
          icon={TrendingUp}
          tone="emerald"
        />
        <MetricTile
          title="Net Revenue"
          value={formatCurrency(sales?.netRevenue)}
          subtext="Recognized after discounts"
          icon={CircleDollarSign}
          tone="blue"
        />
        <MetricTile
          title="Discounts"
          value={formatCurrency(sales?.discounts)}
          subtext="Coupons and commercial offers"
          icon={TrendingDown}
          tone="amber"
        />
        <MetricTile
          title="Average Order"
          value={formatCurrency(sales?.averageOrderValue)}
          subtext="Revenue per completed basket"
          icon={ShoppingBag}
          tone="violet"
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.65fr)_minmax(280px,0.95fr)]">
        <ChartCard title="Revenue Trend" subtitle="Daily revenue movement across the active reporting window">
          <TimelineChart points={sales?.timeline || []} />
        </ChartCard>
        <ChartCard title="Revenue by Status" subtitle="Commercial value grouped by order state">
          <BarList
            items={(sales?.statusRevenue || []).map((item) => ({
              label: item.status,
              value: item.revenue,
              labelValue: formatCurrency(item.revenue)
            }))}
            tone="emerald"
          />
        </ChartCard>
      </div>
    </SectionShell>
  );
}

function OrdersSection({ report }) {
  const orders = report?.orderReport;

  return (
    <SectionShell
      title="Order Analytics"
      description="Track how orders are flowing through the business and where operational pressure is building."
      icon={ReceiptText}
      tone="blue"
      meta={[
        { label: 'Completion rate', value: formatPercent(orders?.fulfillmentRate) },
        { label: 'Recent activity', value: `${orders?.recentOrders?.length || 0} logged orders` }
      ]}
    >
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <MetricTile title="Total Volume" value={formatNumber(orders?.totalOrders)} subtext="Orders in window" icon={ReceiptText} tone="blue" />
        <MetricTile title="Open Pipeline" value={formatNumber(orders?.openOrders)} subtext="Awaiting completion" icon={RefreshCw} tone="amber" />
        <MetricTile title="Completed" value={formatNumber(orders?.completedOrders)} subtext="Successfully delivered" icon={TrendingUp} tone="emerald" />
        <MetricTile title="Cancelled" value={formatNumber(orders?.cancelledOrders)} subtext="Orders not fulfilled" icon={TrendingDown} tone="rose" />
        <MetricTile title="Success Rate" value={formatPercent(orders?.fulfillmentRate)} subtext="Completion percentage" icon={BarChart3} tone="violet" />
      </div>

      <div className="grid gap-6 xl:grid-cols-2">
        <ChartCard title="Stage Distribution" subtitle="Orders at each stage of fulfillment">
          <BarList
            items={(orders?.statusBreakdown || []).map((item) => ({
              label: item.status,
              value: item.count,
              labelValue: `${formatNumber(item.count)} orders`
            }))}
            tone="blue"
          />
        </ChartCard>
        <ChartCard title="Geographic Spread" subtitle="Top delivery cities ranked by order volume">
          <BarList
            items={(orders?.topDestinations || []).map((item) => ({
              label: item.label,
              value: item.count,
              labelValue: `${formatNumber(item.count)} orders`
            }))}
            tone="violet"
          />
        </ChartCard>
      </div>

      <ChartCard title="Operational Log" subtitle="Most recent orders captured inside the reporting window">
        <DataTable
          columns={['Ref ID', 'Customer', 'Status', 'Items', 'Destination', 'Revenue', 'Date']}
          rows={(orders?.recentOrders || []).map((item) => [
            <span key="id" className="font-mono text-xs font-semibold text-slate-500">{shortOrderId(item.orderId)}</span>,
            <span key="cust" className="font-medium text-slate-700">{item.customerEmail || '-'}</span>,
            <StatusBadge key="stat" status={item.status} />,
            <span key="items" className="text-slate-600">{item.items}</span>,
            <span key="city" className="inline-flex items-center gap-1 text-slate-600"><MapPin className="h-3 w-3" />{item.city || '-'}</span>,
            <span key="rev" className="font-semibold text-slate-900">{formatCurrency(item.total)}</span>,
            <span key="date" className="text-slate-500">{formatDateTime(item.createdAt)}</span>
          ])}
          emptyText="No recent orders found for this reporting window."
        />
      </ChartCard>
    </SectionShell>
  );
}

function ProductsSection({ report }) {
  const products = report?.productReport;

  return (
    <SectionShell
      title="Catalog Performance"
      description="Monitor the breadth of the catalog, the performance leaders, and value concentration by category."
      icon={ShoppingBag}
      tone="amber"
      meta={[
        { label: 'Active products', value: formatNumber(products?.publishedProducts) },
        { label: 'Inventory value', value: formatCurrency(products?.inventoryValue) }
      ]}
    >
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <MetricTile title="Total SKUs" value={formatNumber(products?.totalProducts)} subtext="Catalog size" icon={Boxes} tone="slate" />
        <MetricTile title="Live" value={formatNumber(products?.publishedProducts)} subtext="Published on storefront" icon={TrendingUp} tone="emerald" />
        <MetricTile title="Featured" value={formatNumber(products?.featuredProducts)} subtext="Promoted items" icon={ArrowUpRight} tone="blue" />
        <MetricTile title="Dormant" value={formatNumber(products?.unsoldProducts)} subtext="No sales in window" icon={TrendingDown} tone="rose" />
        <MetricTile title="Catalog Value" value={formatCurrency(products?.inventoryValue)} subtext="Current asset base" icon={Warehouse} tone="violet" />
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.55fr)_minmax(280px,0.95fr)]">
        <ChartCard title="Top Selling Products" subtitle="Top products ranked by revenue contribution">
          <DataTable
            columns={['Product', 'Category', 'Units', 'Revenue']}
            rows={(products?.bestSellingProducts || []).map((item) => [
              <ProductCell key="p" item={item} />,
              <span key="c" className="text-slate-500">{item.category || '-'}</span>,
              <span key="u" className="font-semibold text-slate-800">{formatNumber(item.unitsSold)}</span>,
              <span key="r" className="font-semibold text-emerald-700">{formatCurrency(item.revenue)}</span>
            ])}
            emptyText="No product sales recorded for this window."
          />
        </ChartCard>
        <ChartCard title="Category Mix" subtitle="Revenue concentration across product categories">
          <BarList
            items={(products?.categoryPerformance || []).map((item) => ({
              label: item.category,
              value: item.revenue,
              labelValue: formatCurrency(item.revenue)
            }))}
            tone="amber"
          />
        </ChartCard>
      </div>
    </SectionShell>
  );
}

function CustomersSection({ report }) {
  const customers = report?.customerReport;

  return (
    <SectionShell
      title="Customer Insights"
      description="A professional view of customer depth, repeat behavior, and the accounts driving the most value."
      icon={Users2}
      tone="violet"
      meta={[
        { label: 'Repeat rate', value: formatPercent(customers?.repeatCustomerRate) },
        { label: 'Average LTV', value: formatCurrency(customers?.averageCustomerValue) }
      ]}
    >
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <MetricTile title="Total Users" value={formatNumber(customers?.totalCustomers)} subtext="Registered customer base" icon={Users2} tone="slate" />
        <MetricTile title="Active" value={formatNumber(customers?.activeCustomers)} subtext="Placed orders in window" icon={TrendingUp} tone="emerald" />
        <MetricTile title="Returning" value={formatNumber(customers?.repeatCustomers)} subtext="Multiple-order customers" icon={RefreshCw} tone="blue" />
        <MetricTile title="Stickiness" value={formatPercent(customers?.repeatCustomerRate)} subtext="Repeat customer rate" icon={ArrowUpRight} tone="amber" />
        <MetricTile title="Avg. LTV" value={formatCurrency(customers?.averageCustomerValue)} subtext="Value per customer" icon={CircleDollarSign} tone="rose" />
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(280px,0.9fr)]">
        <ChartCard title="Key Accounts" subtitle="Highest spending customers recorded in the current period">
          <DataTable
            columns={['Customer', 'Orders', 'Spend', 'Last Order']}
            rows={(customers?.topCustomers || []).map((item) => [
              <div key="id">
                <p className="font-semibold text-slate-800">{item.customerName || 'Guest'}</p>
                <p className="text-xs text-slate-500">{item.customerEmail}</p>
              </div>,
              <span key="ord" className="font-medium text-slate-700">{formatNumber(item.orders)} orders</span>,
              <span key="spd" className="font-semibold text-emerald-700">{formatCurrency(item.spend)}</span>,
              <span key="date" className="text-slate-500">{formatDateTime(item.lastOrderAt)}</span>
            ])}
            emptyText="No active customer spend captured for this period."
          />
        </ChartCard>
        <ChartCard title="User Geography" subtitle="Locations with the highest customer concentration">
          <BarList
            items={(customers?.topCustomerCities || []).map((item) => ({
              label: item.label,
              value: item.count,
              labelValue: `${formatNumber(item.count)} users`
            }))}
            tone="violet"
          />
        </ChartCard>
      </div>
    </SectionShell>
  );
}

function StockSection({ report }) {
  const stock = report?.stockReport;

  return (
    <SectionShell
      title="Inventory Control"
      description="Focus attention on replenishment risk, stock outages, and items approaching expiry."
      icon={Warehouse}
      tone="rose"
      meta={[
        { label: 'Low stock', value: formatNumber(stock?.lowStockProducts) },
        { label: 'Expiring soon', value: formatNumber(stock?.expiringSoonProducts) }
      ]}
    >
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
        <MetricTile title="Tracked SKUs" value={formatNumber(stock?.totalTrackedProducts)} subtext="Inventory with stock tracking" icon={Boxes} tone="slate" />
        <MetricTile title="Healthy" value={formatNumber(stock?.inStockProducts)} subtext="Products with enough stock" icon={TrendingUp} tone="emerald" />
        <MetricTile title="Low Risk" value={formatNumber(stock?.lowStockProducts)} subtext="Under 20 units on hand" icon={TrendingDown} tone="amber" />
        <MetricTile title="Out of Stock" value={formatNumber(stock?.outOfStockProducts)} subtext="Items at zero units" icon={PackageOpen} tone="rose" />
        <MetricTile title="Expiring" value={formatNumber(stock?.expiringSoonProducts)} subtext="Near expiry threshold" icon={CalendarRange} tone="violet" />
        <MetricTile title="Total Value" value={formatCurrency(stock?.inventoryValue)} subtext="On-hand inventory value" icon={CircleDollarSign} tone="blue" />
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        <ChartCard title="Low Stock" subtitle="Items approaching replenishment thresholds">
          <CompactStockList items={stock?.lowStockItems || []} type="warning" />
        </ChartCard>
        <ChartCard title="Stock Out" subtitle="Items currently unavailable for sale">
          <CompactStockList items={stock?.outOfStockItems || []} type="danger" />
        </ChartCard>
        <ChartCard title="Expiry Risk" subtitle="Items nearing or beyond safe sell dates">
          <CompactStockList items={stock?.expiringSoonItems || []} type="info" />
        </ChartCard>
      </div>
    </SectionShell>
  );
}

function SectionShell({ title, description, icon: Icon, tone, meta, children }) {
  const style = toneStyles[tone] || toneStyles.slate;

  return (
    <section className="bg-white rounded border border-gray-100 shadow-sm flex flex-col overflow-hidden">
      <div className={`px-5 py-5 sm:px-6`}>
        <div className="flex flex-col gap-5 border-b border-slate-100 pb-5 xl:flex-row xl:items-start xl:justify-between">
          <div className="flex items-start gap-4">
            <div className={`rounded-2xl p-3 ${style.iconBg} ${style.icon}`}>
              <Icon className="h-6 w-6" />
            </div>
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">Performance View</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{title}</h2>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">{description}</p>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            {meta.map((item) => (
              <div key={item.label} className="min-w-[150px] rounded border border-gray-100 bg-white px-4 py-3 shadow-sm">
                <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{item.label}</p>
                <p className="mt-1 text-sm font-semibold text-slate-900">{item.value}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="mt-6 space-y-6">{children}</div>
      </div>
    </section>
  );
}

function ChartCard({ title, subtitle, children }) {
  return (
    <div className="rounded border border-gray-100 bg-white p-5 shadow-sm">
      <div className="mb-5">
        <h3 className="text-sm font-semibold uppercase tracking-[0.22em] text-slate-500">{title}</h3>
        <p className="mt-2 text-sm text-slate-600">{subtitle}</p>
      </div>
      {children}
    </div>
  );
}

function HeadlineMetricCard({ metric }) {
  const style = toneStyles[metric.tone] || toneStyles.slate;
  const Icon = metric.icon;

  return (
    <div className="rounded border border-gray-100 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500">{metric.title}</p>
          <p className="mt-3 text-[1.85rem] font-semibold tracking-tight text-slate-950">{metric.value}</p>
        </div>
        <div className={`rounded-2xl p-3 ${style.iconBg} ${style.icon}`}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
      <p className="mt-3 text-sm font-medium text-slate-600">{metric.detail}</p>
    </div>
  );
}

function MetricTile({ title, value, subtext, icon: Icon, tone }) {
  const style = toneStyles[tone] || toneStyles.slate;

  return (
    <div className="rounded border border-gray-100 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <div className={`rounded-2xl p-2.5 ${style.iconBg} ${style.icon}`}>
          <Icon className="h-4 w-4" />
        </div>
      </div>
      <p className="mt-4 text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500">{title}</p>
      <p className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{value}</p>
      <p className="mt-2 text-sm leading-6 text-slate-600">{subtext}</p>
    </div>
  );
}

function TimelineChart({ points }) {
  const containerRef = useRef(null);

  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollLeft = containerRef.current.scrollWidth;
    }
  }, [points]);

  if (!points || points.length === 0) {
    return <EmptyState text="No trend data available for this period." />;
  }

  const maxValue = Math.max(...points.map((point) => Number(point.value) || 0), 1);
  const chartHeight = 220;
  const chartWidth = Math.max(points.length * 72, 620);
  const lastIndex = Math.max(points.length - 1, 1);
  const usableHeight = chartHeight - 56;
  const leftPadding = 16;
  const rightPadding = 16;
  const bottomPadding = 24;
  const topPadding = 16;

  const getPointX = (index) => leftPadding + (index / lastIndex) * (chartWidth - leftPadding - rightPadding);
  const getPointY = (value) => {
    const normalizedValue = Number(value) || 0;
    return topPadding + usableHeight - (normalizedValue / maxValue) * usableHeight;
  };

  const pathData = points.reduce((acc, point, index) => {
    const x = getPointX(index);
    const y = getPointY(point.value);
    return `${acc}${index === 0 ? 'M' : 'L'} ${x} ${y} `;
  }, '');

  const areaData = `${pathData}L ${getPointX(lastIndex)} ${chartHeight - bottomPadding} L ${getPointX(0)} ${chartHeight - bottomPadding} Z`;
  const gridFractions = [1, 0.75, 0.5, 0.25, 0];

  return (
    <div className="grid grid-cols-[42px_minmax(0,1fr)] gap-3">
      <div className="flex h-[220px] flex-col justify-between pb-6 pt-3 text-right text-[11px] font-medium text-slate-400">
        {gridFractions.map((fraction) => (
          <span key={fraction}>{formatCompactNumber(maxValue * fraction)}</span>
        ))}
      </div>

      <div ref={containerRef} className="overflow-x-auto pb-2">
        <div className="min-w-max">
          <svg width={chartWidth} height={chartHeight} className="overflow-visible">
            {gridFractions.map((fraction) => {
              const y = topPadding + usableHeight - usableHeight * fraction;
              return (
                <line
                  key={fraction}
                  x1={leftPadding}
                  y1={y}
                  x2={chartWidth - rightPadding}
                  y2={y}
                  stroke="#dbe3ee"
                  strokeDasharray="4 6"
                />
              );
            })}

            <defs>
              <linearGradient id="report-area-gradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#2563eb" stopOpacity="0.22" />
                <stop offset="100%" stopColor="#2563eb" stopOpacity="0.02" />
              </linearGradient>
            </defs>

            <path d={areaData} fill="url(#report-area-gradient)" />
            <path
              d={pathData}
              fill="none"
              stroke="#2563eb"
              strokeWidth="3"
              strokeLinecap="round"
              strokeLinejoin="round"
            />

            {points.map((point, index) => {
              const x = getPointX(index);
              const y = getPointY(point.value);
              return (
                <g key={`${point.label}-${index}`} className="group">
                  <circle cx={x} cy={y} r="4.5" fill="white" stroke="#2563eb" strokeWidth="2" />
                  <circle cx={x} cy={y} r="10" fill="transparent" />
                  <g className="opacity-0 transition-opacity group-hover:opacity-100">
                    <rect
                      x={x - 44}
                      y={y - 42}
                      width="88"
                      height="24"
                      rx="12"
                      fill="#0f172a"
                      opacity="0.94"
                    />
                    <text x={x} y={y - 26} textAnchor="middle" className="fill-white text-[10px] font-semibold">
                      {formatCurrency(point.value)}
                    </text>
                  </g>
                </g>
              );
            })}
          </svg>

          <div className="mt-4 flex justify-between px-1">
            {points.map((point, index) => (
              <div key={`${point.label}-${index}-axis`} className="w-[72px] text-center">
                <p className="text-[11px] font-semibold text-slate-600">{point.label}</p>
                <p className="mt-1 text-[10px] text-slate-400">{formatNumber(point.orders)} orders</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function BarList({ items, tone }) {
  const style = toneStyles[tone] || toneStyles.blue;
  const maxValue = Math.max(...items.map((item) => Number(item.value) || 0), 1);

  if (!items.length) return <EmptyState text="No data to compare." />;

  return (
    <div className="space-y-4">
      {items.map((item) => {
        const percentage = Math.round(((Number(item.value) || 0) / maxValue) * 100);
        return (
          <div key={item.label} className="space-y-2">
            <div className="flex items-center justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-slate-800">{item.label}</p>
                <p className="text-[11px] text-slate-400">{percentage}% of category peak</p>
              </div>
              <span className="text-sm font-semibold text-slate-600">{item.labelValue}</span>
            </div>
            <div className="h-2.5 overflow-hidden rounded-full bg-slate-200/80">
              <div
                className={`h-full rounded-full bg-gradient-to-r ${style.bar}`}
                style={{ width: `${percentage}%` }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}

function DataTable({ columns, rows, emptyText }) {
  return (
    <div className="overflow-hidden rounded border border-gray-100 bg-white">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[720px] text-left">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50/90">
              {columns.map((col) => (
                <th key={col} className="px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center text-sm text-slate-400">
                  {emptyText}
                </td>
              </tr>
            ) : (
              rows.map((row, index) => (
                <tr key={index} className="transition-colors hover:bg-slate-50/80">
                  {row.map((cell, cellIndex) => (
                    <td key={cellIndex} className="px-4 py-3.5 text-sm align-middle">
                      {cell}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function StatusBadge({ status }) {
  const config = {
    DELIVERED: 'bg-emerald-50 text-emerald-700 border-emerald-100',
    CANCELLED: 'bg-rose-50 text-rose-700 border-rose-100',
    PROCESSING: 'bg-amber-50 text-amber-700 border-amber-100',
    SHIPPED: 'bg-blue-50 text-blue-700 border-blue-100',
    DEFAULT: 'bg-slate-50 text-slate-700 border-slate-100'
  };

  const style = config[status] || config.DEFAULT;

  return (
    <span className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold ${style}`}>
      {status}
    </span>
  );
}

function ProductCell({ item }) {
  return (
    <div className="flex items-center gap-3">
      {item.imageUrl ? (
        <img src={item.imageUrl} alt={item.name} className="h-10 w-10 rounded-xl border border-slate-200 object-cover" />
      ) : (
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-[9px] font-semibold text-slate-400">
          IMG
        </div>
      )}
      <div className="min-w-0">
        <p className="truncate font-semibold text-slate-800">{item.name}</p>
        <p className="text-[11px] text-slate-500">{item.productCode || '-'}</p>
      </div>
    </div>
  );
}

function CompactStockList({ items, type }) {
  const configs = {
    warning: { bg: 'bg-amber-50', text: 'text-amber-800', border: 'border-amber-100', badge: 'bg-amber-100 text-amber-700' },
    danger: { bg: 'bg-rose-50', text: 'text-rose-800', border: 'border-rose-100', badge: 'bg-rose-100 text-rose-700' },
    info: { bg: 'bg-blue-50', text: 'text-blue-800', border: 'border-blue-100', badge: 'bg-blue-100 text-blue-700' }
  };

  const config = configs[type] || configs.info;

  if (!items.length) return <EmptyState text="Clear of notices." />;

  return (
    <div className="space-y-3">
      {items.slice(0, 5).map((item, index) => (
        <div key={index} className={`rounded-[20px] border p-4 ${config.bg} ${config.border}`}>
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className={`truncate text-sm font-semibold ${config.text}`}>{item.name}</p>
              <p className="mt-1 text-[11px] text-slate-500">{item.productCode || '-'} • {item.category || 'General'}</p>
            </div>
            <span className={`shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold ${config.badge}`}>
              {formatNumber(item.stockQuantity)} units
            </span>
          </div>
          <div className="mt-3 flex items-center justify-between text-[11px] text-slate-500">
            <span>Expiry</span>
            <span>{item.expiryDate || 'N/A'}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

function InsightPanel({ title, subtitle, tone, items }) {
  const style = toneStyles[tone] || toneStyles.slate;

  return (
    <section className="rounded border border-gray-100 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500">Snapshot</p>
          <h3 className="mt-2 text-lg font-semibold text-slate-950">{title}</h3>
        </div>
        <div className={`h-3 w-3 rounded-full ${style.dot}`} />
      </div>
      <p className="mt-2 text-sm leading-6 text-slate-600">{subtitle}</p>

      <div className="mt-5 space-y-3">
        {items.map((item) => (
          <div key={item.label} className="rounded border border-gray-100 bg-gray-50 px-4 py-3">
            <div className="flex items-center justify-between gap-3">
              <span className="text-sm font-medium text-slate-600">{item.label}</span>
              <span className="text-sm font-semibold text-slate-950">{item.value}</span>
            </div>
            {item.caption ? <p className="mt-2 text-[11px] leading-5 text-slate-500">{item.caption}</p> : null}
          </div>
        ))}
      </div>
    </section>
  );
}

function NarrativePanel({ title, notes, periodLabel }) {
  return (
    <section className="rounded border border-gray-100 bg-white p-5 shadow-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500">Summary</p>
      <h3 className="mt-2 text-lg font-semibold text-slate-950">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-600">
        Management-ready notes for <span className="font-semibold text-slate-900">{periodLabel || 'the current window'}</span>.
      </p>

      <div className="mt-5 space-y-3">
        {notes.map((note, index) => (
          <div key={index} className="rounded border border-gray-100 bg-gray-50 px-4 py-3">
            <div className="flex items-start gap-3">
              <span className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-[#1E3A5F]" />
              <p className="text-sm leading-6 text-slate-700">{note}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function LoadingState() {
  return (
    <div className="flex h-72 flex-col items-center justify-center rounded-[30px] border border-slate-200 bg-white shadow-sm">
      <LoaderCircle className="h-10 w-10 animate-spin text-[#1E3A5F]" />
      <p className="mt-4 text-sm font-medium text-slate-500">Compiling report data...</p>
    </div>
  );
}

function ErrorState({ error, onRetry }) {
  return (
    <div className="rounded-[30px] border border-rose-100 bg-white p-8 shadow-sm">
      <p className="text-lg font-semibold text-rose-900">Something went wrong</p>
      <p className="mt-2 text-sm leading-6 text-rose-700">{error}</p>
      <button
        onClick={onRetry}
        className="mt-6 inline-flex items-center gap-2 rounded-2xl bg-rose-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-rose-700"
      >
        <RefreshCw className="h-4 w-4" />
        Try Again
      </button>
    </div>
  );
}

function EmptyState({ text }) {
  return (
    <div className="flex items-center justify-center rounded-[22px] border border-dashed border-slate-200 bg-white/70 px-4 py-10 text-center text-sm text-slate-400">
      {text}
    </div>
  );
}

function getHeadlineMetrics(report) {
  const overview = report?.overview;
  const sales = report?.salesReport;
  const orders = report?.orderReport;
  const customers = report?.customerReport;
  const stock = report?.stockReport;

  return [
    {
      title: 'Net Revenue',
      value: formatCurrency(overview?.totalRevenue),
      detail: `Gross revenue ${formatCurrency(sales?.grossRevenue)}`,
      icon: CircleDollarSign,
      tone: 'emerald'
    },
    {
      title: 'Total Orders',
      value: formatNumber(overview?.totalOrders),
      detail: `${formatNumber(orders?.completedOrders)} completed across the period`,
      icon: ReceiptText,
      tone: 'blue'
    },
    {
      title: 'Customer Base',
      value: formatNumber(overview?.totalCustomers),
      detail: `${formatNumber(customers?.repeatCustomers)} repeat buyers active`,
      icon: Users2,
      tone: 'violet'
    },
    {
      title: 'Inventory Watch',
      value: formatNumber(overview?.lowStockProducts),
      detail: `${formatNumber(stock?.outOfStockProducts)} products currently unavailable`,
      icon: Warehouse,
      tone: 'amber'
    }
  ];
}

function getSectionStats(report, activeSection) {
  const sales = report?.salesReport;
  const orders = report?.orderReport;
  const products = report?.productReport;
  const customers = report?.customerReport;
  const stock = report?.stockReport;

  const configs = {
    sales: [
      { label: 'Net revenue', value: formatCurrency(sales?.netRevenue), caption: 'Recognized after discounting.' },
      { label: 'Discount load', value: formatCurrency(sales?.discounts), caption: 'Promotional pressure inside the period.' },
      { label: 'Average order', value: formatCurrency(sales?.averageOrderValue), caption: 'Revenue earned per order.' }
    ],
    orders: [
      { label: 'Open pipeline', value: formatNumber(orders?.openOrders), caption: 'Orders still moving through operations.' },
      { label: 'Completed', value: formatNumber(orders?.completedOrders), caption: 'Orders fulfilled successfully.' },
      { label: 'Success rate', value: formatPercent(orders?.fulfillmentRate), caption: 'Completion percentage of total volume.' }
    ],
    products: [
      { label: 'Live catalog', value: formatNumber(products?.publishedProducts), caption: 'Products currently available for sale.' },
      { label: 'Featured items', value: formatNumber(products?.featuredProducts), caption: 'Merchandising-led product count.' },
      { label: 'Catalog value', value: formatCurrency(products?.inventoryValue), caption: 'Estimated value of tracked catalog stock.' }
    ],
    customers: [
      { label: 'Active buyers', value: formatNumber(customers?.activeCustomers), caption: 'Customers who placed orders in window.' },
      { label: 'Repeat buyers', value: formatNumber(customers?.repeatCustomers), caption: 'Customers returning for multiple orders.' },
      { label: 'Average LTV', value: formatCurrency(customers?.averageCustomerValue), caption: 'Average customer value realized.' }
    ],
    stock: [
      { label: 'Low stock', value: formatNumber(stock?.lowStockProducts), caption: 'Products below the safety threshold.' },
      { label: 'Stockout', value: formatNumber(stock?.outOfStockProducts), caption: 'Products with no stock available.' },
      { label: 'Expiry risk', value: formatNumber(stock?.expiringSoonProducts), caption: 'Items needing expiry review soon.' }
    ]
  };

  return configs[activeSection] || configs.sales;
}

function getSectionNarrative(report, activeSection) {
  const sales = report?.salesReport;
  const orders = report?.orderReport;
  const products = report?.productReport;
  const customers = report?.customerReport;
  const stock = report?.stockReport;

  const narratives = {
    sales: [
      `Revenue checkpoints captured: ${formatNumber(sales?.timeline?.length)} across the active reporting window.`,
      `Discount impact is ${formatCurrency(sales?.discounts)} against gross revenue of ${formatCurrency(sales?.grossRevenue)}.`,
      `Average order value stands at ${formatCurrency(sales?.averageOrderValue)}, helping frame customer basket quality.`
    ],
    orders: [
      `A total of ${formatNumber(orders?.totalOrders)} orders were recorded, with ${formatNumber(orders?.completedOrders)} completed successfully.`,
      `The open pipeline currently holds ${formatNumber(orders?.openOrders)} orders that still require operational attention.`,
      `Fulfillment performance is running at ${formatPercent(orders?.fulfillmentRate)}, while cancellations are ${formatNumber(orders?.cancelledOrders)}.`
    ],
    products: [
      `${formatNumber(products?.publishedProducts)} products are live from a total catalog of ${formatNumber(products?.totalProducts)} SKUs.`,
      `${formatNumber(products?.unsoldProducts)} items remain dormant in the current window and may need merchandising attention.`,
      `Tracked catalog value is ${formatCurrency(products?.inventoryValue)}, offering a clear view of commercial asset weight.`
    ],
    customers: [
      `${formatNumber(customers?.activeCustomers)} active buyers contributed in the period from a base of ${formatNumber(customers?.totalCustomers)} customers.`,
      `${formatNumber(customers?.repeatCustomers)} customers returned to place additional orders, with repeat rate at ${formatPercent(customers?.repeatCustomerRate)}.`,
      `Average customer value is ${formatCurrency(customers?.averageCustomerValue)}, providing a quick benchmark for account quality.`
    ],
    stock: [
      `${formatNumber(stock?.lowStockProducts)} products are below threshold and ${formatNumber(stock?.outOfStockProducts)} are already unavailable.`,
      `${formatNumber(stock?.expiringSoonProducts)} items are nearing expiry and should be reviewed alongside replenishment planning.`,
      `Inventory currently under management is valued at ${formatCurrency(stock?.inventoryValue)} across ${formatNumber(stock?.totalTrackedProducts)} tracked SKUs.`
    ]
  };

  return narratives[activeSection] || narratives.sales;
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0
  }).format(Number(value || 0));
}

function formatNumber(value) {
  return new Intl.NumberFormat('en-IN').format(Number(value || 0));
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`;
}

function formatCompactNumber(value) {
  return new Intl.NumberFormat('en-IN', {
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(Number(value || 0));
}

function formatDateTime(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function formatTimestamp(value) {
  if (!value) return 'just now';
  return value.toLocaleTimeString('en-IN', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

function shortOrderId(orderId) {
  if (!orderId) return '-';
  return orderId.length > 8 ? `...${orderId.slice(-6)}` : orderId;
}
