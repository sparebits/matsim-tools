import {ComponentFixture, TestBed} from '@angular/core/testing';

import {RoutesDialogComponent} from './routes-dialog.component';

describe('RoutesDialogComponent', () => {
    let component: RoutesDialogComponent;
    let fixture: ComponentFixture<RoutesDialogComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RoutesDialogComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(RoutesDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
