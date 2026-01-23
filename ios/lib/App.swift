//
//  App.swift
//  ios
//
//  Created by Anas Erkinjonov on 23/01/26.
//


import SwiftUI
import Shared

public struct AppView: View {
    
    @State
    var stringsLoaded = false
    
    public init() {}

    public var body: some View {
        VStack {
            if(stringsLoaded){
                Image(systemName: "globe")
                    .imageScale(.large)
                    .foregroundStyle(.tint)
                Text("Hello, world!")
                AuthorCell(author: getSampleAuthor(), onClick: {
                    // Handle author cell click here if needed
                })
            }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
        .padding()
        .onAppear {
            if !stringsLoaded {
                Resource.shared.setLocale(locale: "uz", onLoad: {
                    stringsLoaded = true
                })
            }
        }
    }
}
