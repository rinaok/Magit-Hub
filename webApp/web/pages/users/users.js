var msgVersion = 0;
var refreshRate = 2000; //milli seconds
var USER_LIST_URL = buildUrlWithContextPath("userslist");
var GET_ACTIVE_USER = 1;
var GET_USERLIST = 2;
var CLONE = 3;
var CLEAN_LOCAL = 4;

function refreshUsersList(users) {
    $.each(users || [], function(index, username) {
        var hasOption = $('#usersList option[value="' + username + '"]');
        if (hasOption.length == 0){
            var option = $('<option>', {value: username, text: username});
            option.appendTo($("#usersList"));
        }
    });
    if(!$('#usersList').val()[0])
        $('#usersList option[value="' + document.getElementById('activeUser').innerHTML + '"]').prop({selected: true});
}

$(function() { // onload...do
    var data = "reqType=" + GET_ACTIVE_USER;
    $.ajax({
        method: 'GET',
        url: "users",
        data: data,
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            document.getElementById('activeUser').innerHTML = r;
        }
    });
    $('#usersList').change(onUserListChanged);
});

$(function() { // onload...do
    var data = "reqType=" + CLEAN_LOCAL;
    $.ajax({
        method: 'GET',
        url: "users",
        data: data,
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            if(r)
                localStorage.clear();
        }
    });
});

function onUserListChanged(){
    var username = $('#usersList').val()[0];
    getRepositoryByName(username);
}

// function appendToMsgArea(entries) {
//     // add the relevant entries
//     $.each(entries || [], appendMsgEntry);
//
//     var scroller = $("#msgarea");
//     var height = scroller.scrollTop() - $(scroller).height();
//     $(scroller).stop().animate({ scrollTop: height }, "slow");
//     messages = $("#msgarea")[0].innerHTML;
// }
//
// function appendMsgEntry(index, entry){
//     var entryElement = createMsgEntry(entry);
//     $("#msgarea").append(entryElement).append("<br>");
// }
//
// function createMsgEntry (entry){
//     return $("<span class=\"success\">").append("<b>" + entry.timestamp + "> </b>" + entry.message);
// }

function ajaxUsersList(){
    var data = "reqType=" + GET_USERLIST;
    $.ajax({
        url: USER_LIST_URL,
        data: data,
        success: function(users) {
            refreshUsersList(users);
        }
    });
}

// function ajaxMsgContent() {
//     var username = document.getElementById('activeUser').innerHTML;
//     msgVersion = getSessionItem(username);
//     $.ajax({
//         url: "messages",
//         data: "msgVersion=" + msgVersion,
//         success: function(data) {
//             console.log("Server msg version: " + data.version + ", Current msg version: " + msgVersion);
//             if (data.version != msgVersion) {
//                 msgVersion = data.version;
//                 setSessionItem(username, msgVersion);
//                 appendToMsgArea(data.entries);
//             }
//             triggerAjaxMsgContent();
//         },
//         error: function(error) {
//             triggerAjaxMsgContent();
//         }
//     });
// }
//
// $(function() { // onload...do
//     //add a function to the submit event
//     $("#msgform").submit(function() {
//         $.ajax({
//             data: $(this).serialize(),
//             url: "getMessages",
//             timeout: 2000,
//             error: function() {
//                 console.error("Failed to submit");
//             },
//             success: function(r) {
//                 //since it's going to be retrieved from the client.server
//                 //$("#result h1").text(r);
//             }
//         });
//
//         $("#messages").val("");
//         // by default - we'll always return false so it doesn't redirect the user.
//         return false;
//     });
// });
//
// function triggerAjaxMsgContent() {
//     setTimeout(ajaxMsgContent, refreshRate);
// }

//activate the timer calls after the page is loaded
$(function() {
    //The users list is refreshed automatically every second
    setInterval(ajaxUsersList, refreshRate);

    //on each call it triggers another execution of itself later (1 second later)
    triggerAjaxMsgContent();
});

$(function() { // onload...do
    getRepositories();
});

function getRepositories(){
    $("#activeUser").val("")
    $("#uploadForm").change(function() {
        var file = this[0].files[0];
        var formData = new FormData();
        formData.append("fake-key-1", file);
        $.ajax({
            method:'POST',
            data: formData,
            url: this.action,
            processData: false, // Don't process the files
            contentType: false, // Set content type to false as jQuery will tell the server its a query string request
            timeout: 4000,
            error: function(e) {
                alert(e.responseText);
            },
            success: function(r) {
                updateRepositoriesTable(r);
            }
        });
        // return value of the submit operation
        // by default - we'll always return false so it doesn't redirect the user.
        return false;
    });
}

function getRepositoryByName(username) {
    $.ajax({
        method: 'PUT',
        data: username,
        url: "upload",
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            $('#repositoriesTable tbody').empty();
            if(r != null) {
                for (var i = 0; i < r.length; i++) {
                    updateRepositoriesTable(JSON.parse(r[i]), username);
                }
            }
        }
    });
}

function insertForkedRepositoryToList(){
    var username = document.getElementById('activeUser').innerHTML;
    $.ajax({
        method: 'GET',
        data: username,
        url: "upload",
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            $('#repositoriesTable tbody').empty();
            for (var i = 0 ; i < r.length; i++) {
                updateRepositoriesTable(JSON.parse(r[i]), username);
            }
        }
    });
}

function fork(repoOwner, repoName){
    var req = "reqType=" + CLONE + "&owner=" + repoOwner + "&repoName=" + repoName;
    $.ajax({
        method: 'GET',
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        data: req,
        url: "users",
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            insertForkedRepositoryToList();
            $.ajax({
                method: 'POST',
                data: repoName + "&" + document.getElementById('activeUser').innerHTML,
                url: "repo",
                processData: false, // Don't process the files
                contentType: false, // Set content type to false as jQuery will tell the server its a query string request
                timeout: 4000,
                error: function (e) {
                    alert(e.responseText);
                },
                success: function (r) {
                    window.location = '../repository/repository.html'
                }
            });
        }
    });
}

function updateRepositoriesTable(r){
    var owner = $('#usersList').val()[0];
    var loggedInUser = document.getElementById('activeUser').innerHTML;
    if(owner == null)
        owner = loggedInUser;
    var htmlString = '<tr class="child" style=\"cursor:pointer\"><td>'
        + r["Name"] + '</td><td>'
        + r["ActiveBranch"] + '</td><td>'
        + r["BranchesAmount"] + '</td><td>'
        + r["CommitDate"] + '</td><td>'
        + r["CommitMessage"] + '</td><td>'
        + r["Forked"] + '</td></tr>';
    $('#repositoriesTable tbody').append(htmlString);
    var rowElement = $.parseHTML(htmlString)
    var table = document.getElementById("repositoriesTable");
    if (table != null) {
        for (var i = 0; i < table.rows.length; i++) {
            for (var j = 0; j < table.rows[i].cells.length; j++)
            if (loggedInUser == owner) { // logged in user
                    table.rows[i].onclick = function (e) {
                        var repoName = e.currentTarget.firstChild.innerText;
                        $.ajax({
                            method: 'POST',
                            data: repoName + "&" + owner,
                            url: "repo",
                            processData: false, // Don't process the files
                            contentType: false, // Set content type to false as jQuery will tell the server its a query string request
                            timeout: 4000,
                            error: function (e) {
                                alert(e.responseText);
                            },
                            success: function (r) {
                                window.location = '../repository/repository.html'
                            }
                        });
                    }
                }
            else{
                table.rows[i].onclick = function (ev) {
                    var repoName = ev.currentTarget.firstChild.innerText;
                    var answer = confirm("You cannot open this repository since it's owned by another user. " +
                        "Would you like to fork it?");
                    if(answer){
                        fork(owner, repoName);
                    }
                }
            }
        }
    }
}

$(function() {
    var interval = setInterval(function() {
        if(document.getElementById("activeUser").innerHTML) {
            clearInterval(interval);
            getRepositoryByName(document.getElementById("activeUser").innerHTML);
        }
    }, 1000);
});




function setSessionItem(name, value) {
    var mySession;
    try {
        mySession = JSON.parse(localStorage.getItem('mySession'));
    } catch (e) {
        console.log(e);
        mySession = {};
    }

    mySession[name] = value;

    mySession = JSON.stringify(mySession);

    localStorage.setItem('mySession', mySession);
}

function getSessionItem(name) {
    if(window.localStorage) {
        var mySession = localStorage.getItem('mySession');
        if (mySession) {
            try {
                mySession = JSON.parse(mySession);
                return mySession[name];
            } catch (e) {
                console.log(e);
            }
        }
    }
}

function restoreSession(data) {
    for (var x in data) {
        //use saved data to set values as needed
        console.log(x, data[x]);
    }
}

function addMsgListener() {
        var mySession = localStorage.getItem('mySession');
        if (mySession) {
            try {
                mySession = JSON.parse(localStorage.getItem('mySession'));
            } catch (e) {
                console.log(e);
                mySession = {};
            }
            restoreSession(mySession);
        } else {
            localStorage.setItem('mySession', '{}');
        }

        var username = document.getElementById('activeUser').innerHTML;
        if (!mySession[username]) {
            setSessionItem(username, 0); //should not change on refresh
        }
}

$(function() {
    var interval = setInterval(function() {
        if(document.getElementById("activeUser").innerHTML) {
            clearInterval(interval);
            addMsgListener();
        }
    }, 1000);
});

function ajaxMsgContent() {
    var username = document.getElementById('activeUser').innerHTML;
    msgVersion = getSessionItem(username);
    $.ajax({
        url: "messages",
        data: "msgVersion=" + msgVersion,
        success: function(data) {
            console.log("Server msg version: " + data.version + ", Current msg version: " + msgVersion);
            if (data.version != msgVersion) {
                msgVersion = data.version;
                setSessionItem(username, msgVersion);
                appendToMsgArea(data.entries);
            }
            triggerAjaxMsgContent();
        },
        error: function(error) {
            triggerAjaxMsgContent();
        }
    });
}