//
//  ObservableViewModel.swift
//  iosApp
//
//  Created by Anas Erkinjonov on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

extension ObservableViewModel {
    func binding(
        getValue: @escaping (State) -> String,
        getIntent: @escaping (String) -> Intent
    ) -> Binding<String> {
        Binding { [self] in
            getValue(state)
        } set: { newValue in
            self.handle(intent: getIntent(newValue))
        }
    }

    func binding(
        getValue: @escaping (State) -> String?,
        getIntent: @escaping (String?) -> Intent
    ) -> Binding<String?> {
        Binding<String?>(
            get: { [self] in getValue(state) },
            set: { newValue in self.handle(intent: getIntent(newValue)) }
        )
    }
}

@Observable
class ObservableViewModel<
    State: BaseState, Intent: BaseIntent, Event: BaseEvent, VM: BaseViewModel<State, Intent, Event>
> {

    let viewModel: VM

    var state: State
    var event: Event?

    init() {
        self.viewModel = inject()
        self.state = viewModel.state.value
    }

    func handle(intent: Intent) {
        viewModel.handle(intent: intent)
    }

    func collect() async {
        async let eventTask: Void = collectEvents()
        async let stateTask: Void = collectState()

        _ = await (eventTask, stateTask)
    }

    private func collectState() async {
        for await newState in viewModel.state {
            state = newState
        }
    }

    private func collectEvents() async {
        for await newEvent in viewModel.eventsFlow {
            event = newEvent
        }
    }

}
