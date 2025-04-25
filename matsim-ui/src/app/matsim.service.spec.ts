import {TestBed} from '@angular/core/testing';

import {MATSimService} from './matsim.service';

describe('MATSimService', () => {
    let service: MATSimService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(MATSimService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
