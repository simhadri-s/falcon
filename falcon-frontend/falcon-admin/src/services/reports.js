import api from './api';

const downloadBlob = (blob, fileName) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
};

const parseFileName = (contentDisposition, period) => {
  if (!contentDisposition) {
    return `Management_Report_${period}.pdf`;
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }

  const asciiMatch = contentDisposition.match(/filename="?([^"]+)"?/i);
  if (asciiMatch?.[1]) {
    return asciiMatch[1];
  }

  return `Management_Report_${period}.pdf`;
};

export const downloadManagementReport = async (period) => {
  try {
    const response = await api.get('/api/reports/download', {
      params: { period },
      responseType: 'blob',
    });

    const fileName = parseFileName(
      response.headers['content-disposition'],
      period,
    );

    downloadBlob(response.data, fileName);
    return fileName;
  } catch (error) {
    console.error('Failed to download report:', error);
    throw new Error('Failed to download PDF report. Please try again.');
  }
};
