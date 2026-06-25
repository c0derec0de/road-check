/** @type {import('jest').Config} */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "jsdom",
  roots: ["<rootDir>/src"],
  setupFilesAfterEnv: ["<rootDir>/src/test/setupTests.ts"],
  transform: {
    "^.+\\.(ts|tsx)$": ["ts-jest", { tsconfig: "tsconfig.jest.json" }],
  },
  moduleFileExtensions: ["ts", "tsx", "js", "jsx", "json"],
  testPathIgnorePatterns: ["/node_modules/", "/dist/"],
  clearMocks: true,
  reporters: [
    "default",
    [
      "jest-junit",
      { outputDirectory: "test-results", outputName: "junit.xml" },
    ],
  ],
  collectCoverage: true,
  coverageDirectory: "coverage",
  coverageReporters: ["text", "html", "cobertura"],
};
