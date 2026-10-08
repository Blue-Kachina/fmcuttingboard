// The one place .fmcalc features get their language facts from: the fmscriptinventory catalogue vendored at
// shared/data/fm-calc-catalogue.json (docs/fm-calc-catalogue-contract.md). Functions go through
// FileMakerFunctionRegistry; scripts/generate-grammar.mjs reads the same file.
import language from '../../../shared/data/fm-calc-catalogue.json';
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
