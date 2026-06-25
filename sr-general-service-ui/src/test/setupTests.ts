import '@testing-library/jest-dom';
import { TextDecoder, TextEncoder } from 'util';
import { jest } from '@jest/globals';

Object.assign(global, { TextEncoder, TextDecoder });
Object.assign(global, { jest });
