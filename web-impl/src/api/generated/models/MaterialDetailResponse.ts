/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CostInfo } from './CostInfo';
import type { DrawingInfo } from './DrawingInfo';
import type { LaborInfo } from './LaborInfo';
import type { MaterialBaseInfo } from './MaterialBaseInfo';
import type { OutsourceInfo } from './OutsourceInfo';
import type { PriceInfo } from './PriceInfo';
import type { ProcessInfo } from './ProcessInfo';
export type MaterialDetailResponse = {
    base?: MaterialBaseInfo;
    process?: ProcessInfo;
    drawing?: DrawingInfo;
    price?: PriceInfo;
    cost?: CostInfo;
    labor?: LaborInfo;
    outsource?: OutsourceInfo;
};

