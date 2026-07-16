const WEBP_OUTPUT_TYPE = 'image/webp';
const HD_MAX_DIMENSION = 1280;
const DEFAULT_WEBP_QUALITY = 0.82;

const loadImageElement = (file) =>
  new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const image = new Image();

    image.onload = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(image);
    };

    image.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(new Error(`Failed to read image "${file.name}".`));
    };

    image.src = objectUrl;
  });

const getScaledDimensions = (width, height, maxDimension) => {
  if (width <= maxDimension && height <= maxDimension) {
    return { width, height };
  }

  const scale = Math.min(maxDimension / width, maxDimension / height);

  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  };
};

const toWebpBlob = (canvas, quality) =>
  new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error('Image compression failed.'));
          return;
        }

        resolve(blob);
      },
      WEBP_OUTPUT_TYPE,
      quality
    );
  });

const toWebpFileName = (fileName) =>
  fileName.replace(/\.[^.]+$/, '') + '.webp';

export const compressImageToHdWebp = async (
  file,
  {
    maxDimension = HD_MAX_DIMENSION,
    quality = DEFAULT_WEBP_QUALITY,
  } = {}
) => {
  if (!(file instanceof File)) {
    throw new Error('Expected an image file for compression.');
  }

  const image = await loadImageElement(file);
  const { width, height } = getScaledDimensions(
    image.naturalWidth || image.width,
    image.naturalHeight || image.height,
    maxDimension
  );

  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;

  const context = canvas.getContext('2d', { alpha: true });
  if (!context) {
    throw new Error('Image compression is not supported in this browser.');
  }

  context.drawImage(image, 0, 0, width, height);

  const webpBlob = await toWebpBlob(canvas, quality);
  return new File([webpBlob], toWebpFileName(file.name), {
    type: WEBP_OUTPUT_TYPE,
    lastModified: Date.now(),
  });
};

export const compressImagesToHdWebp = async (files, options) =>
  Promise.all(files.map((file) => compressImageToHdWebp(file, options)));
