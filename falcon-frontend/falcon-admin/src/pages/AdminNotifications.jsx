import { useState } from 'react';
import { Bell, Send, Megaphone, Loader2, CheckCircle2, AlertCircle, Tag, Package } from 'lucide-react';
import api from '../services/api';
import { compressImageToHdWebp } from '../utils/imageCompression';

const NOTIFICATION_TEMPLATES = [
  {
    label: 'Flash Sale',
    icon: Tag,
    title: '🔥 Flash Sale is Live!',
    body: 'Get up to 50% off on all laser cutting services. Limited time only!',
    type: 'OFFER',
  },
  {
    label: 'New Products',
    icon: Package,
    title: '✨ New Products Available',
    body: 'We have added exciting new products to our catalog. Check them out now!',
    type: 'ANNOUNCEMENT',
  },
  {
    label: 'Custom',
    icon: Megaphone,
    title: '',
    body: '',
    type: 'ANNOUNCEMENT',
  },
];

export default function AdminNotifications() {
  const [form, setForm] = useState({ title: '', body: '', imageUrl: '', type: 'ANNOUNCEMENT' });
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState(null); // { type: 'success' | 'error', message }
  const [selectedTemplate, setSelectedTemplate] = useState(2); // custom by default

  const handleTemplateSelect = (idx) => {
    const t = NOTIFICATION_TEMPLATES[idx];
    setSelectedTemplate(idx);
    setForm(f => ({ ...f, title: t.title, body: t.body }));
    setResult(null);
  };

  const handleChange = (e) => {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }));
    setResult(null);
  };

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file);
      setImagePreview(URL.createObjectURL(file));
      setForm(f => ({ ...f, imageUrl: '' })); // clear url if file is selected
    }
  };

  const handleSend = async () => {
    if (!form.title.trim() || !form.body.trim()) {
      setResult({ type: 'error', message: 'Title and body are required.' });
      return;
    }

    setSending(true);
    setResult(null);

    try {
      const formData = new FormData();
      const compressedImage = imageFile
        ? await compressImageToHdWebp(imageFile)
        : null;
      formData.append('title', form.title);
      formData.append('body', form.body);
      if (compressedImage) {
        formData.append('image', compressedImage);
      } else if (form.imageUrl) {
        formData.append('imageUrl', form.imageUrl);
      }
      formData.append('data[type]', form.type);

      await api.post('/api/admin/notifications/broadcast', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      setResult({ type: 'success', message: 'Notification broadcast successfully to all users!' });
      setForm({ title: '', body: '', imageUrl: '', type: 'ANNOUNCEMENT' });
      setImageFile(null);
      setImagePreview(null);
      setSelectedTemplate(2);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to send notification.';
      setResult({ type: 'error', message: msg });
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="relative flex flex-col h-full p-6 space-y-6">
      {/* Page Header */}
      <div className="flex items-center gap-4">
        <div className="p-2 rounded bg-blue-600 text-white shadow-sm">
          <Bell className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Push Notifications</h1>
          <p className="text-[0.85rem] text-gray-500 mt-0.5">Send real-time notifications to all app users</p>
        </div>
      </div>

      {/* Template Selector */}
      <div className="bg-white rounded shadow-sm border border-gray-100 p-6">
        <h2 className="text-[0.85rem] font-semibold text-gray-700 mb-4">Quick Templates</h2>
        <div className="grid grid-cols-3 gap-3">
          {NOTIFICATION_TEMPLATES.map((t, idx) => {
            const Icon = t.icon;
            const isActive = selectedTemplate === idx;
            return (
              <button
                key={idx}
                onClick={() => handleTemplateSelect(idx)}
                className={`flex flex-col items-center justify-center p-4 rounded transition-all duration-200 gap-2 cursor-pointer border ${
                  isActive
                    ? 'border-blue-400 bg-blue-50 text-blue-600 shadow-sm'
                    : 'border-gray-200 bg-gray-50 text-gray-500 hover:border-gray-300 hover:bg-white'
                }`}
              >
                <Icon className="w-5 h-5" />
                <span className="text-[0.85rem] font-medium">{t.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Compose Form */}
      <div className="bg-white rounded shadow-sm border border-gray-100 p-6 space-y-5">
        <h2 className="text-[0.85rem] font-semibold text-gray-700">Compose Notification</h2>

        {/* Type Badge */}
        <div>
          <label className="block text-[0.85rem] font-medium text-gray-700 mb-2">Notification Type</label>
          <div className="flex gap-3">
            {['OFFER', 'ANNOUNCEMENT', 'UPDATE'].map(type => (
              <button
                key={type}
                onClick={() => setForm(f => ({ ...f, type }))}
                className={`rounded text-[0.85rem] font-medium transition-colors shadow-sm px-3 py-1.5 ${
                  form.type === type
                    ? 'bg-blue-600 hover:bg-blue-700 text-white'
                    : 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-50'
                }`}
              >
                {type}
              </button>
            ))}
          </div>
        </div>

        {/* Title */}
        <div>
          <label className="block text-[0.85rem] font-medium text-gray-700 mb-1.5">
            Title <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            name="title"
            value={form.title}
            onChange={handleChange}
            placeholder="e.g. 🔥 Flash Sale is Live!"
            maxLength={100}
            className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
          />
          <div className="text-right text-[0.75rem] text-gray-400 mt-1">{form.title.length}/100</div>
        </div>

        {/* Body */}
        <div>
          <label className="block text-[0.85rem] font-medium text-gray-700 mb-1.5">
            Message <span className="text-red-500">*</span>
          </label>
          <textarea
            name="body"
            value={form.body}
            onChange={handleChange}
            placeholder="Write your notification message here..."
            maxLength={300}
            rows={4}
            className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white resize-none"
          />
          <div className="text-right text-[0.75rem] text-gray-400 mt-1">{form.body.length}/300</div>
        </div>

        {/* Image Upload */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-[0.85rem] font-medium text-gray-700 mb-1.5">
              Upload Image <span className="text-gray-400 font-normal">(optional)</span>
            </label>
            <div className="flex items-center gap-3">
              <label className="flex-1 flex flex-col items-center justify-center px-4 py-4 bg-white text-gray-500 rounded border border-dashed border-gray-300 cursor-pointer hover:border-blue-400 hover:text-blue-500 transition-all">
                <Package className="w-6 h-6 mb-2" />
                <span className="text-[0.75rem] font-medium">Click to upload image</span>
                <input type="file" className="hidden" accept="image/*" onChange={handleImageChange} />
              </label>
            </div>
          </div>
          <div>
            <label className="block text-[0.85rem] font-medium text-gray-700 mb-1.5">
              Or Image URL
            </label>
            <input
              type="url"
              name="imageUrl"
              value={form.imageUrl}
              onChange={handleChange}
              placeholder="https://example.com/promo-image.jpg"
              className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white"
            />
          </div>
        </div>

        {/* Result Banner */}
        {result && (
          <div className={`flex items-start gap-3 p-4 rounded text-[0.85rem] font-medium ${
            result.type === 'success'
              ? 'bg-green-50 text-green-800 border border-green-200'
              : 'bg-red-50 text-red-700 border border-red-200'
          }`}>
            {result.type === 'success'
              ? <CheckCircle2 className="w-4 h-4 flex-shrink-0 mt-0.5" />
              : <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
            }
            {result.message}
          </div>
        )}

        {/* Preview */}
        {(form.title || form.body) && (
          <div className="bg-gray-50 rounded p-4 border border-dashed border-gray-200">
            <p className="text-[0.75rem] font-semibold text-gray-500 uppercase tracking-wider mb-3">Live Preview</p>
            <div className="bg-white rounded shadow-sm overflow-hidden max-w-sm mx-auto border border-gray-100">
              {(imagePreview || form.imageUrl) && (
                <div className="aspect-video w-full bg-gray-100 overflow-hidden">
                  <img 
                    src={imagePreview || form.imageUrl} 
                    alt="Preview" 
                    className="w-full h-full object-cover"
                    onError={(e) => { e.target.style.display = 'none'; }}
                  />
                </div>
              )}
              <div className="p-4 flex items-start gap-3">
                <div className="w-8 h-8 rounded bg-blue-600 flex items-center justify-center flex-shrink-0">
                  <Bell className="w-4 h-4 text-white" />
                </div>
                <div className="min-w-0">
                  <p className="font-semibold text-[0.85rem] text-gray-800 leading-tight">
                    {form.title || 'Notification Title'}
                  </p>
                  <p className="text-[0.85rem] text-gray-600 mt-1 leading-relaxed line-clamp-2">
                    {form.body || 'Notification message will appear here...'}
                  </p>
                  <p className="text-[0.75rem] text-gray-400 mt-1.5">just now</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Send Button */}
        <button
          onClick={handleSend}
          disabled={sending}
          className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2"
        >
          {sending ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Broadcasting...
            </>
          ) : (
            <>
              <Send className="w-4 h-4" />
              Send to All Users
            </>
          )}
        </button>
      </div>

      {/* Info Box */}
      <div className="bg-blue-50 border border-blue-100 rounded p-4 flex items-start gap-3">
        <Bell className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
        <div>
          <p className="text-[0.85rem] font-semibold text-blue-800">Broadcast to All Users</p>
          <p className="text-[0.85rem] text-blue-600 mt-1 leading-relaxed">
            This notification will be delivered to every registered user who has push notifications enabled. 
            Notifications are also stored in user history for in-app viewing.
          </p>
        </div>
      </div>
    </div>
  );
}
