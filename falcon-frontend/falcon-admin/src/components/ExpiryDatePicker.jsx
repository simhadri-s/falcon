import { useState, useRef, useEffect } from 'react';
import { Calendar, ChevronLeft, ChevronRight } from 'lucide-react';

export default function ExpiryDatePicker({ value, onChange, placeholder = "MM-YYYY or DD-MM-YYYY", className = "" }) {
  const [isOpen, setIsOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('month'); // 'day' or 'month'
  const [currentYear, setCurrentYear] = useState(new Date().getFullYear());
  const [currentMonth, setCurrentMonth] = useState(new Date().getMonth()); // 0-11
  
  const popoverRef = useRef(null);

  // Parse existing value on open to initialize visual state
  useEffect(() => {
    if (isOpen && value) {
      const parts = value.split('-');
      if (parts.length === 3) {
        // DD-MM-YYYY
        const [d, m, y] = parts;
        setActiveTab('day');
        if (!isNaN(y)) setCurrentYear(parseInt(y, 10));
        if (!isNaN(m)) setCurrentMonth(parseInt(m, 10) - 1);
      } else if (parts.length === 2) {
        // MM-YYYY
        const [m, y] = parts;
        setActiveTab('month');
        if (!isNaN(y)) setCurrentYear(parseInt(y, 10));
        if (!isNaN(m)) setCurrentMonth(parseInt(m, 10) - 1);
      }
    }
  }, [isOpen, value]);

  // Click outside to close
  useEffect(() => {
    function handleClickOutside(event) {
      if (popoverRef.current && !popoverRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const months = [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  ];

  const handleMonthSelect = (monthIndex) => {
    const mm = String(monthIndex + 1).padStart(2, '0');
    const newValue = `${mm}-${currentYear}`;
    onChange(newValue);
    setIsOpen(false);
  };

  const handleDaySelect = (day) => {
    const dd = String(day).padStart(2, '0');
    const mm = String(currentMonth + 1).padStart(2, '0');
    const newValue = `${dd}-${mm}-${currentYear}`;
    onChange(newValue);
    setIsOpen(false);
  };

  // Helper to get days in month
  const getDaysInMonth = (year, month) => {
    return new Date(year, month + 1, 0).getDate();
  };

  // Helper to get first day of month (0 = Sunday, etc.)
  const getFirstDayOfMonth = (year, month) => {
    return new Date(year, month, 1).getDay();
  };

  const daysInMonth = getDaysInMonth(currentYear, currentMonth);
  const firstDay = getFirstDayOfMonth(currentYear, currentMonth);

  const daysArray = [];
  // Empty blocks for offset
  for (let i = 0; i < firstDay; i++) {
    daysArray.push(null);
  }
  // Days of the month
  for (let i = 1; i <= daysInMonth; i++) {
    daysArray.push(i);
  }

  return (
    <div className="relative w-full" ref={popoverRef}>
      <div className="relative flex items-center">
        <input
          type="text"
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => setIsOpen(true)}
          onClick={() => setIsOpen(true)}
          placeholder={placeholder}
          className={`${className} w-full pr-10`}
        />
        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className="absolute right-3 text-gray-400 hover:text-blue-500 transition-colors focus:outline-none"
        >
          <Calendar className="w-4 h-4" />
        </button>
      </div>

      {isOpen && (
        <div className="absolute right-0 mt-1 z-50 bg-white border border-gray-200 rounded shadow-xl p-4 w-72 select-none">
          {/* Tab selector */}
          <div className="flex border-b border-gray-100 mb-3 text-xs">
            <button
              type="button"
              onClick={() => setActiveTab('month')}
              className={`flex-1 pb-1.5 font-medium transition-all ${
                activeTab === 'month' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-800'
              }`}
            >
              Month Only
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('day')}
              className={`flex-1 pb-1.5 font-medium transition-all ${
                activeTab === 'day' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-800'
              }`}
            >
              Specific Day
            </button>
          </div>

          {activeTab === 'month' ? (
            <div>
              {/* Year Selector header */}
              <div className="flex items-center justify-between mb-3">
                <button
                  type="button"
                  onClick={() => setCurrentYear(currentYear - 1)}
                  className="p-1 hover:bg-gray-100 rounded text-gray-500 hover:text-gray-800 transition-colors"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <span className="font-semibold text-gray-700 text-sm">{currentYear}</span>
                <button
                  type="button"
                  onClick={() => setCurrentYear(currentYear + 1)}
                  className="p-1 hover:bg-gray-100 rounded text-gray-500 hover:text-gray-800 transition-colors"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>

              {/* Month Grid */}
              <div className="grid grid-cols-3 gap-2">
                {months.map((m, idx) => {
                  const isCurrent = value && value === `${String(idx + 1).padStart(2, '0')}-${currentYear}`;
                  return (
                    <button
                      key={m}
                      type="button"
                      onClick={() => handleMonthSelect(idx)}
                      className={`py-1.5 rounded text-xs transition-colors font-medium ${
                        isCurrent
                          ? 'bg-blue-600 text-white font-semibold'
                          : 'hover:bg-blue-50 text-gray-700'
                      }`}
                    >
                      {m}
                    </button>
                  );
                })}
              </div>
            </div>
          ) : (
            <div>
              {/* Month and Year navigation */}
              <div className="flex items-center justify-between mb-3">
                <button
                  type="button"
                  onClick={() => {
                    if (currentMonth === 0) {
                      setCurrentMonth(11);
                      setCurrentYear(currentYear - 1);
                    } else {
                      setCurrentMonth(currentMonth - 1);
                    }
                  }}
                  className="p-1 hover:bg-gray-100 rounded text-gray-500 hover:text-gray-800 transition-colors"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <span className="font-semibold text-gray-700 text-sm">
                  {months[currentMonth]} {currentYear}
                </span>
                <button
                  type="button"
                  onClick={() => {
                    if (currentMonth === 11) {
                      setCurrentMonth(0);
                      setCurrentYear(currentYear + 1);
                    } else {
                      setCurrentMonth(currentMonth + 1);
                    }
                  }}
                  className="p-1 hover:bg-gray-100 rounded text-gray-500 hover:text-gray-800 transition-colors"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>

              {/* Day names */}
              <div className="grid grid-cols-7 gap-1 text-[10px] text-gray-400 font-semibold text-center mb-1">
                <span>Su</span><span>Mo</span><span>Tu</span><span>We</span><span>Th</span><span>Fr</span><span>Sa</span>
              </div>

              {/* Days Grid */}
              <div className="grid grid-cols-7 gap-1 text-center">
                {daysArray.map((day, idx) => {
                  if (day === null) return <div key={`empty-${idx}`} />;
                  const isCurrent = value && value === `${String(day).padStart(2, '0')}-${String(currentMonth + 1).padStart(2, '0')}-${currentYear}`;
                  return (
                    <button
                      key={day}
                      type="button"
                      onClick={() => handleDaySelect(day)}
                      className={`py-1 rounded text-xs transition-colors font-medium ${
                        isCurrent
                          ? 'bg-blue-600 text-white font-semibold shadow-sm'
                          : 'hover:bg-blue-50 text-gray-700'
                      }`}
                    >
                      {day}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
          
          {/* Quick options */}
          <div className="mt-3 pt-2 border-t border-gray-100 flex justify-between">
            <button
              type="button"
              onClick={() => { onChange(''); setIsOpen(false); }}
              className="text-xs text-red-500 hover:text-red-700 font-medium transition-colors"
            >
              Clear Date
            </button>
            <button
              type="button"
              onClick={() => setIsOpen(false)}
              className="text-xs text-gray-500 hover:text-gray-700 font-medium transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
