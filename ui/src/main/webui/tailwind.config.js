/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  corePlugins: {
    preflight: false, // Avoid conflicts with PrimeNG and existing styles
  },
  theme: {
    extend: {
      screens: {
        xs: { max: '540px' },
      },
    },
  },
  plugins: [],
};
