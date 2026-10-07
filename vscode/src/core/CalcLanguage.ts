// The one place .fmcalc features get their language facts from. Today: functions from
// shared/data/filemaker-functions.json, everything else from shared/data/calc-language.json. When the
// fmscriptinventory catalogue is vendored (shared/data/fm-calc-catalogue.json, docs/fm-calc-catalogue-contract.md),
// switch these imports to it; callers don't change. scripts/generate-grammar.mjs reads the same files.
import language from '../../../shared/data/calc-language.json';
import { findByName, getAll, type FunctionMetadata } from './FileMakerFunctionRegistry';

export interface CalcConstant {
  name: string;
  group: string;
  value: string | number | null;
}

export function functions(): FunctionMetadata[] {
  return getAll();
}

export function findFunction(name: string): FunctionMetadata | undefined {
  return findByName(name);
}

export function getConstantNames(): string[] {
  return language.getConstants.map((c) => c.name);
}

export function constants(): CalcConstant[] {
  return language.constants.map((c) => ({ name: c.name, group: c.group, value: c.value }));
}

export function findConstant(name: string): CalcConstant | undefined {
  const lower = name.toLowerCase();
  return constants().find((c) => c.name.toLowerCase() === lower);
}

export function isGetConstant(name: string): boolean {
  const lower = name.toLowerCase();
  return getConstantNames().some((n) => n.toLowerCase() === lower);
}
