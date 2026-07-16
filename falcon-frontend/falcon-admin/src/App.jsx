import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import AdminLogin from './pages/AdminLogin.jsx';
import AdminDashboard from './pages/AdminDashboard';
import AdminProducts from './pages/AdminProducts';
import AdminProductDetail from './pages/AdminProductDetail';
import AdminInquiries from './pages/AdminInquiries';
import InquiryDetail from './pages/InquiryDetail';
import AdminNews from './pages/AdminNews';
import AdminCareers from './pages/AdminCareers';
import AdminRegister from './pages/AdminRegister';
import AdminUsers from './pages/AdminUsers';
import AdminIndustries from './pages/AdminIndustries';
import SettingsDashboard from './pages/SettingsDashboard';
import AdminBanners from './pages/AdminBanners';
import AdminDeliveryLocations from './pages/AdminDeliveryLocations';
import AdminOrders from './pages/AdminOrders';
import AdminCustomers from './pages/AdminCustomers';
import AdminOrderDetail from './pages/AdminOrderDetail';
import CompanySettings from './pages/CompanySettings';
import CompanyImages from './pages/CompanyImages';
import MailerSettings from './pages/MailerSettings';
import WhatsappSettings from './pages/WhatsappSettings';
import AdminNotifications from './pages/AdminNotifications';
import AdminCoupons from './pages/AdminCoupons';
import AdminCategories from './pages/AdminCategories';
import AdminReviews from './pages/AdminReviews';
import AdminReturns from './pages/AdminReturns';
import AdminReturnDetail from './pages/AdminReturnDetail';
import AdminRefunds from './pages/AdminRefunds';
import AdminReports from './pages/AdminReports';

import { CompanyProvider } from './context/CompanyContext';
import { getTokenCookie, isTokenExpired } from './utils/cookies';


const ProtectedRoute = ({ children }) => {
  const token = getTokenCookie();

  // Legacy localStorage cleanup if present
  if (localStorage.getItem('token')) {
    localStorage.removeItem('token');
  }

  if (!token || isTokenExpired(token)) {
    return <Navigate to="/admin/login" replace />;
  }
  return children;
};

function App() {
  return (
    <CompanyProvider>
      <Router>
        <Routes>
          <Route path="/admin/login" element={<AdminLogin />} />
          <Route path="/admin/register" element={<AdminRegister />} />

          {/* Protected Admin Routes */}
          <Route path="/admin" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
            <Route path="dashboard" element={<AdminDashboard />} />
            <Route path="products" element={<AdminProducts />} />
            <Route path="products/:slug" element={<AdminProductDetail />} />
            <Route path="inquiries" element={<AdminInquiries />} />
            <Route path="inquiries/:id" element={<InquiryDetail />} />
            <Route path="news" element={<AdminNews />} />
            <Route path="careers" element={<AdminCareers />} />
            <Route path="users" element={<AdminUsers />} />
            <Route path="customers" element={<AdminCustomers />} />
            <Route path="industries" element={<AdminIndustries />} />
            <Route path="orders" element={<AdminOrders />} />
            <Route path="orders/:id" element={<AdminOrderDetail />} />
            <Route path="reports" element={<AdminReports />} />

            <Route path="settings" element={<SettingsDashboard />} />
            <Route path="settings/banners" element={<AdminBanners />} />
            <Route path="settings/locations" element={<AdminDeliveryLocations />} />
            <Route path="settings/company-details" element={<CompanySettings />} />
            <Route path="settings/company-images" element={<CompanyImages />} />
            <Route path="settings/mailer-settings" element={<MailerSettings />} />
            <Route path="settings/whatsapp-settings" element={<WhatsappSettings />} />
            <Route path="notifications" element={<AdminNotifications />} />
            <Route path="coupons" element={<AdminCoupons />} />
            <Route path="returns" element={<AdminReturns />} />
            <Route path="returns/:id" element={<AdminReturnDetail />} />
            <Route path="refunds" element={<AdminRefunds />} />
            <Route path="reviews" element={<AdminReviews />} />
            <Route path="settings/categories" element={<AdminCategories />} />
          </Route>

          <Route path="*" element={<Navigate to="/admin/login" />} />
        </Routes>
      </Router>
    </CompanyProvider>
  );
}

export default App;
