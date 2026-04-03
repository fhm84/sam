/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  corePlugins: {
    preflight: false, // Avoid conflicts with PrimeNG and existing styles
  },
  theme: {
    extend: {},
  },
  plugins: [],
};
